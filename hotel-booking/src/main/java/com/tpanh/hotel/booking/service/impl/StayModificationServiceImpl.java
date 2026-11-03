package com.tpanh.hotel.booking.service.impl;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingNight;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.request.EarlyCheckoutRequest;
import com.tpanh.hotel.booking.dto.request.ExtendStayRequest;
import com.tpanh.hotel.booking.dto.response.AssignedRoomResponse;
import com.tpanh.hotel.booking.dto.response.EarlyCheckoutResponse;
import com.tpanh.hotel.booking.dto.response.ExtendStayResponse;
import com.tpanh.hotel.booking.exception.BookingNotFoundException;
import com.tpanh.hotel.booking.exception.StayModificationException;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.booking.service.StayModificationService;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.payment.domain.Payment;
import com.tpanh.hotel.payment.domain.PaymentRefund;
import com.tpanh.hotel.payment.domain.PaymentRefundStatus;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import com.tpanh.hotel.payment.domain.PaymentTransaction;
import com.tpanh.hotel.payment.domain.PaymentTransactionStatus;
import com.tpanh.hotel.payment.domain.PaymentTransactionType;
import com.tpanh.hotel.payment.repository.PaymentRepository;
import com.tpanh.hotel.payment.service.CounterPaymentService;
import com.tpanh.hotel.pricing.dto.result.RoomPricingResult;
import com.tpanh.hotel.pricing.service.PricingService;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import com.tpanh.hotel.room.service.RoomAvailabilityCalculator;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.exception.UserNotFoundException;
import com.tpanh.hotel.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StayModificationServiceImpl implements StayModificationService {
    private static final int EARLY_CHECKOUT_PENALTY_PERCENT = 20;
    private static final String CURRENCY = "VND";
    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomAvailabilityCalculator availabilityCalculator;
    private final PricingService pricingService;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;
    private final CounterPaymentService counterPaymentService;

    @Override
    @Transactional(readOnly = true)
    public EarlyCheckoutResponse quoteEarlyCheckout(UUID bookingId, LocalDate actualCheckOutDate) {
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Không tìm thấy đặt phòng"));
        validateEarlyCheckout(booking, actualCheckOutDate);
        return earlyCheckoutCalculation(booking, actualCheckOutDate, null, null);
    }

    @Override
    @Transactional
    public EarlyCheckoutResponse earlyCheckout(UUID bookingId, EarlyCheckoutRequest request) {
        Booking booking = bookingRepository
                .findByIdWithAssignedRoomForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Không tìm thấy đặt phòng"));
        validateEarlyCheckout(booking, request.actualCheckOutDate());
        Room assignedRoom = booking.getAssignedRoom();
        if (assignedRoom == null) {
            throw StayModificationException.conflict("ROOM_NOT_ASSIGNED", "Đặt phòng chưa được gán phòng vật lý");
        }
        Room room = roomRepository
                .findByIdForUpdate(assignedRoom.getId())
                .orElseThrow(() -> StayModificationException.conflict("ROOM_NOT_FOUND", "Không tìm thấy phòng"));
        EarlyCheckoutResponse calculation = earlyCheckoutCalculation(booking, request.actualCheckOutDate(), null, room);
        long additionalChargeAmount = request.normalizedAdditionalChargeAmount();
        long collectedAmount = request.normalizedCollectedAmount();
        String additionalChargeReason = request.normalizedAdditionalChargeReason();
        long balanceBeforeEarlyRefund;
        long finalTotalAmount;
        try {
            finalTotalAmount = Math.addExact(booking.getTotalAmount(), additionalChargeAmount);
            balanceBeforeEarlyRefund = Math.addExact(outstandingAmount(booking), additionalChargeAmount);
        } catch (ArithmeticException exception) {
            throw StayModificationException.badRequest("CHECKOUT_AMOUNT_OVERFLOW", "Tổng tiền vượt giới hạn cho phép");
        }
        long refundAmount = Math.max(calculation.refundAmount() - balanceBeforeEarlyRefund, 0L);
        long requiredCollection = Math.max(balanceBeforeEarlyRefund - calculation.refundAmount(), 0L);
        if (collectedAmount < requiredCollection) {
            throw StayModificationException.conflict(
                    "PAYMENT_REQUIRED", "Còn phải thu " + (requiredCollection - collectedAmount) + " VND");
        }
        if (collectedAmount > requiredCollection) {
            throw StayModificationException.conflict(
                    "CHECKOUT_OVER_COLLECTION", "Số tiền thu thêm vượt số tiền còn phải thu sau khi trừ khoản hoàn");
        }
        if (refundAmount > 0 && request.refundInformation() == null) {
            throw StayModificationException.badRequest(
                    "REFUND_INFORMATION_REQUIRED", "Cần cung cấp thông tin nhận hoàn tiền");
        }
        User receptionist = currentUserProvider
                .currentUserId()
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy tài khoản lễ tân"));
        if (additionalChargeAmount > 0) {
            booking.applyTotalAmount(finalTotalAmount);
        }
        counterPaymentService.settleAtCheckout(booking, collectedAmount, receptionist.getId());
        PaymentRefundStatus refundStatus = null;
        if (refundAmount > 0) {
            Payment payment = paymentRepository
                    .findFirstByBookingIdAndStatusOrderByPaidAtDesc(bookingId, PaymentStatus.SUCCESS)
                    .orElseThrow(() -> StayModificationException.conflict(
                            "INSUFFICIENT_PAID_AMOUNT", "Không có khoản thanh toán thành công để hoàn tiền"));
            PaymentRefund refund = PaymentRefund.builder()
                    .amount(BigDecimal.valueOf(refundAmount))
                    .currency(CURRENCY)
                    .status(PaymentRefundStatus.PENDING)
                    .reason("Trả phòng sớm " + request.actualCheckOutDate())
                    .idempotencyKey("early-checkout:" + bookingId)
                    .refundMethod(request.refundInformation().method())
                    .bankName(request.refundInformation().normalizedBankName())
                    .accountHolderName(request.refundInformation().normalizedAccountHolderName())
                    .accountNumber(request.refundInformation().normalizedAccountNumber())
                    .requestedAt(clock.instant())
                    .build();
            payment.addRefund(refund);
            payment.addTransaction(PaymentTransaction.builder()
                    .type(PaymentTransactionType.REFUND)
                    .status(PaymentTransactionStatus.PENDING)
                    .amount(refundAmount)
                    .currency(CURRENCY)
                    .receivedAt(clock.instant())
                    .build());
            refundStatus = PaymentRefundStatus.PENDING;
        }
        room.markDirtyAfterCheckout();
        booking.checkOut(
                receptionist,
                buildCheckoutNote(
                        request.normalizedNote(), additionalChargeAmount, additionalChargeReason, collectedAmount));
        return withRefundAmount(calculation, refundAmount, refundStatus, room);
    }

    @Override
    @Transactional
    public ExtendStayResponse extendStay(UUID bookingId, ExtendStayRequest request) {
        Booking booking = bookingRepository
                .findByIdWithAssignedRoomForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Không tìm thấy đặt phòng"));
        UUID userId = currentUserProvider
                .currentUserId()
                .orElseThrow(() -> new BookingNotFoundException("Không tìm thấy đặt phòng"));
        if (booking.getBookedBy() == null || !booking.getBookedBy().getId().equals(userId)) {
            throw new BookingNotFoundException("Không tìm thấy đặt phòng");
        }
        if (!ACTIVE_STATUSES.contains(booking.getStatus())) {
            throw StayModificationException.conflict(
                    "BOOKING_NOT_ELIGIBLE_FOR_EXTENSION", "Trạng thái booking không cho phép gia hạn");
        }
        LocalDate oldDate = booking.getCheckOutDate();
        if (!request.newCheckOutDate().isAfter(oldDate)) {
            throw StayModificationException.badRequest(
                    "INVALID_EXTENSION_DATE", "Ngày trả phòng mới phải sau ngày hiện tại");
        }
        roomTypeRepository.findByIdForUpdate(booking.getRoomType().getId()).orElseThrow();
        Room assignedRoom = booking.getAssignedRoom();
        if (assignedRoom != null) {
            roomRepository.findByIdForUpdate(assignedRoom.getId()).orElseThrow();
            if (bookingRepository.countAssignedRoomConflicts(
                            assignedRoom.getId(), bookingId, oldDate, request.newCheckOutDate(), ACTIVE_STATUSES)
                    > 0) {
                throw StayModificationException.conflict(
                        "ROOM_NOT_AVAILABLE_FOR_EXTENSION", "Phòng hiện tại không trống cho toàn bộ thời gian gia hạn");
            }
        } else if (availabilityCalculator.countAvailableRoomsExcludingBooking(
                        booking.getRoomType().getId(), booking.getCheckInDate(), request.newCheckOutDate(), bookingId)
                <= 0) {
            throw StayModificationException.conflict(
                    "ROOM_NOT_AVAILABLE_FOR_EXTENSION", "Hạng phòng không còn trống cho thời gian gia hạn");
        }
        RoomPricingResult pricing =
                pricingService.calculateRoomPrice(booking.getRoomType(), oldDate, request.newCheckOutDate());
        long additionalAmount = pricing.finalRoomAmount();
        booking.extendTo(request.newCheckOutDate(), pricing.nights(), additionalAmount);
        long outstanding = Math.max(booking.getTotalAmount() - booking.getPaidAmount(), 0L);
        return new ExtendStayResponse(
                bookingId,
                booking.getBookingCode(),
                oldDate,
                booking.getCheckOutDate(),
                pricing.nights().size(),
                additionalAmount,
                booking.getTotalAmount(),
                outstanding,
                outstanding > 0,
                assignedRoom == null ? null : assignedRoom.getId(),
                CURRENCY);
    }

    private void validateEarlyCheckout(Booking booking, LocalDate actualDate) {
        if (booking.getStatus() == BookingStatus.CHECKED_OUT) {
            throw StayModificationException.conflict("BOOKING_ALREADY_CHECKED_OUT", "Đặt phòng đã được trả phòng");
        }
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw StayModificationException.conflict(
                    "BOOKING_NOT_ELIGIBLE_FOR_EARLY_CHECKOUT", "Chỉ đặt phòng đang lưu trú mới có thể trả phòng sớm");
        }
        if (actualDate.isBefore(booking.getCheckInDate()) || !actualDate.isBefore(booking.getCheckOutDate())) {
            throw StayModificationException.badRequest(
                    "INVALID_EARLY_CHECKOUT_DATE",
                    "Ngày trả phòng sớm phải từ ngày nhận phòng và trước ngày trả phòng dự kiến");
        }
    }

    private EarlyCheckoutResponse earlyCheckoutCalculation(
            Booking booking, LocalDate actualDate, PaymentRefundStatus status, Room room) {
        long unusedAmount = booking.getNights().stream()
                .filter(night -> !night.getStayDate().isBefore(actualDate))
                .mapToLong(BookingNight::getPricePerNight)
                .sum();
        long unusedNights = booking.getNights().stream()
                .filter(night -> !night.getStayDate().isBefore(actualDate))
                .count();
        long penalty = BigDecimal.valueOf(unusedAmount)
                .multiply(BigDecimal.valueOf(EARLY_CHECKOUT_PENALTY_PERCENT))
                .divide(BigDecimal.valueOf(100))
                .longValueExact();
        long refund = unusedAmount - penalty;
        return new EarlyCheckoutResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getStatus(),
                booking.getCheckOutDate(),
                actualDate,
                unusedNights,
                unusedAmount,
                EARLY_CHECKOUT_PENALTY_PERCENT,
                penalty,
                refund,
                CURRENCY,
                status,
                room == null
                        ? null
                        : new AssignedRoomResponse(
                                room.getId(), room.getRoomNumber(), room.getFloor(), room.getStatus()));
    }

    private long outstandingAmount(Booking booking) {
        return Math.max(booking.getTotalAmount() - booking.getPaidAmount(), 0L);
    }

    private EarlyCheckoutResponse withRefundAmount(
            EarlyCheckoutResponse calculation, long refundAmount, PaymentRefundStatus status, Room room) {
        return new EarlyCheckoutResponse(
                calculation.bookingId(),
                calculation.bookingCode(),
                BookingStatus.CHECKED_OUT,
                calculation.originalCheckOutDate(),
                calculation.actualCheckOutDate(),
                calculation.unusedNights(),
                calculation.unusedAmount(),
                calculation.penaltyPercent(),
                calculation.penaltyAmount(),
                refundAmount,
                calculation.currency(),
                status,
                new AssignedRoomResponse(room.getId(), room.getRoomNumber(), room.getFloor(), room.getStatus()));
    }

    private String buildCheckoutNote(String note, long charge, String reason, long collected) {
        StringBuilder result = new StringBuilder(note == null ? "" : note);
        if (charge > 0) {
            appendNote(result, "Phí phát sinh: " + charge + " VND - " + reason);
        }
        if (collected > 0) {
            appendNote(result, "Đã thu thêm tại quầy: " + collected + " VND");
        }
        return result.isEmpty() ? null : result.toString();
    }

    private void appendNote(StringBuilder result, String value) {
        if (!result.isEmpty()) {
            result.append("; ");
        }
        result.append(value);
    }
}
