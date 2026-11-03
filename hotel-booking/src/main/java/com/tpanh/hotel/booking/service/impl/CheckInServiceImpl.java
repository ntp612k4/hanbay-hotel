package com.tpanh.hotel.booking.service.impl;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.request.CheckInGuestRequest;
import com.tpanh.hotel.booking.dto.response.AssignedRoomResponse;
import com.tpanh.hotel.booking.dto.response.AvailablePhysicalRoomResponse;
import com.tpanh.hotel.booking.dto.response.CheckInGuestResponse;
import com.tpanh.hotel.booking.exception.CheckInBookingNotFoundException;
import com.tpanh.hotel.booking.exception.CheckInConflictException;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.booking.service.CheckInService;
import com.tpanh.hotel.common.security.CurrentUserProvider;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    @Override
    @Transactional
    public CheckInGuestResponse checkIn(String bookingCode, CheckInGuestRequest request) {
        Booking booking = bookingRepository
                .findByBookingCodeWithRoomTypeForUpdate(bookingCode)
                .orElseThrow(() -> new CheckInBookingNotFoundException("Không tìm thấy booking"));

        validateBooking(booking);

        Room room = roomRepository
                .findByIdForUpdate(request.roomId())
                .orElseThrow(() -> new RoomNotFoundException("Không tìm thấy phòng"));

        validateRoom(booking, room);

        User receptionist = currentUserProvider
                .currentUserId()
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy tài khoản lễ tân"));
        Instant checkedInAt = clock.instant();

        room.occupy();
        booking.checkIn(room, receptionist);

        return new CheckInGuestResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getStatus(),
                new AssignedRoomResponse(room.getId(), room.getRoomNumber(), room.getFloor(), room.getStatus()),
                checkedInAt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailablePhysicalRoomResponse> getAvailableRooms(String bookingCode) {
        Booking booking = bookingRepository
                .findByBookingCode(bookingCode)
                .orElseThrow(() -> new CheckInBookingNotFoundException("Không tìm thấy booking"));
        validateBooking(booking);

        return roomRepository
                .findAllByRoomTypeIdAndStatusOrderByFloorAscRoomNumberAsc(
                        booking.getRoomType().getId(), RoomStatus.AVAILABLE)
                .stream()
                .map(room -> new AvailablePhysicalRoomResponse(room.getId(), room.getRoomNumber(), room.getFloor()))
                .toList();
    }

    private void validateBooking(Booking booking) {
        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            throw CheckInConflictException.bookingAlreadyCheckedIn();
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw CheckInConflictException.bookingNotConfirmed();
        }
        if (!booking.getCheckInDate().equals(LocalDate.now(clock))) {
            throw CheckInConflictException.checkInDateNotAllowed();
        }
    }

    private void validateRoom(Booking booking, Room room) {
        if (!booking.getRoomType().getId().equals(room.getRoomType().getId())) {
            throw CheckInConflictException.roomTypeMismatch();
        }
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw CheckInConflictException.roomNotAvailable();
        }
    }
}
