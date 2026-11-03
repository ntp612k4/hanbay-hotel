package com.tpanh.hotel.booking.service.impl;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.request.CheckOutGuestRequest;
import com.tpanh.hotel.booking.dto.response.AssignedRoomResponse;
import com.tpanh.hotel.booking.dto.response.CheckOutGuestResponse;
import com.tpanh.hotel.booking.exception.CheckOutBookingNotFoundException;
import com.tpanh.hotel.booking.exception.CheckOutConflictException;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.booking.service.CheckOutService;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.payment.service.CounterPaymentService;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.exception.RoomNotFoundException;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.exception.UserNotFoundException;
import com.tpanh.hotel.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckOutServiceImpl implements CheckOutService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;
    private final CounterPaymentService counterPaymentService;

    @Override
    @Transactional
    public CheckOutGuestResponse checkOut(UUID bookingId, CheckOutGuestRequest request) {
        Booking booking = bookingRepository
                .findByIdWithAssignedRoomForUpdate(bookingId)
                .orElseThrow(() -> new CheckOutBookingNotFoundException("Không tìm thấy đặt phòng"));

        validateBookingStatus(booking);
        if (LocalDate.now(clock).isBefore(booking.getCheckOutDate())) {
            throw CheckOutConflictException.earlyCheckoutRequired();
        }
        Room assignedRoom = booking.getAssignedRoom();
        if (assignedRoom == null) {
            throw CheckOutConflictException.roomNotAssigned();
        }

        Room room = roomRepository
                .findByIdForUpdate(assignedRoom.getId())
                .orElseThrow(() -> new RoomNotFoundException("Không tìm thấy phòng"));
        validateRoom(room);

        long additionalChargeAmount = request == null ? 0L : request.normalizedAdditionalChargeAmount();
        long collectedAmount = request == null ? 0L : request.normalizedCollectedAmount();
        String additionalChargeReason = request == null ? null : request.normalizedAdditionalChargeReason();
        String note = request == null ? null : request.normalizedNote();

        long finalTotalAmount;
        long finalPaidAmount;
        try {
            finalTotalAmount = Math.addExact(booking.getTotalAmount(), additionalChargeAmount);
            finalPaidAmount = Math.addExact(booking.getPaidAmount(), collectedAmount);
        } catch (ArithmeticException exception) {
            throw new RequestValidationException("additionalChargeAmount", "Tổng tiền vượt giới hạn cho phép");
        }
        if (collectedAmount > outstandingAmount(finalTotalAmount, booking.getPaidAmount())) {
            throw CheckOutConflictException.overCollection();
        }
        long outstandingAmount = outstandingAmount(finalTotalAmount, finalPaidAmount);
        if (outstandingAmount > 0) {
            throw CheckOutConflictException.paymentRequired(outstandingAmount);
        }

        User receptionist = currentUserProvider
                .currentUserId()
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy tài khoản lễ tân"));
        Instant checkedOutAt = clock.instant();

        if (additionalChargeAmount > 0) {
            booking.applyTotalAmount(finalTotalAmount);
        }
        counterPaymentService.settleAtCheckout(booking, collectedAmount, receptionist.getId());
        room.markDirtyAfterCheckout();
        booking.checkOut(
                receptionist, buildCheckOutNote(note, additionalChargeAmount, additionalChargeReason, collectedAmount));

        return new CheckOutGuestResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getStatus(),
                new AssignedRoomResponse(room.getId(), room.getRoomNumber(), room.getFloor(), room.getStatus()),
                checkedOutAt,
                booking.getTotalAmount(),
                booking.getPaidAmount(),
                outstandingAmount(booking),
                additionalChargeAmount,
                additionalChargeReason,
                collectedAmount);
    }

    private void validateBookingStatus(Booking booking) {
        if (booking.getStatus() == BookingStatus.CHECKED_OUT) {
            throw CheckOutConflictException.bookingAlreadyCheckedOut();
        }
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw CheckOutConflictException.bookingNotCheckedIn();
        }
    }

    private void validateRoom(Room room) {
        if (room.getStatus() != RoomStatus.OCCUPIED) {
            throw CheckOutConflictException.roomNotOccupied();
        }
    }

    private long outstandingAmount(Booking booking) {
        return outstandingAmount(booking.getTotalAmount(), booking.getPaidAmount());
    }

    private long outstandingAmount(long totalAmount, long paidAmount) {
        return Math.max(totalAmount - paidAmount, 0L);
    }

    private String buildCheckOutNote(
            String note, long additionalChargeAmount, String additionalChargeReason, long collectedAmount) {
        StringBuilder builder = new StringBuilder();
        if (note != null) {
            builder.append(note);
        }
        if (additionalChargeAmount > 0) {
            appendSeparator(builder);
            builder.append("Phí phát sinh: ")
                    .append(additionalChargeAmount)
                    .append(" VND")
                    .append(" - ")
                    .append(additionalChargeReason);
        }
        if (collectedAmount > 0) {
            appendSeparator(builder);
            builder.append("Đã thu thêm tại quầy: ").append(collectedAmount).append(" VND");
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private void appendSeparator(StringBuilder builder) {
        if (!builder.isEmpty()) {
            builder.append("; ");
        }
    }
}
