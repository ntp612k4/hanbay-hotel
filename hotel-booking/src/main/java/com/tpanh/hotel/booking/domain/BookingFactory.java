package com.tpanh.hotel.booking.domain;

import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingFactory {

    private static final int PAYMENT_EXPIRE_MINUTES = 15;
    private final Clock clock;

    public Booking createPendingBooking(
            User bookedBy,
            String bookingCode,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int adults,
            int children) {

        Objects.requireNonNull(roomType, "Loại phòng không được để trống.");
        Objects.requireNonNull(checkIn, "Ngày nhận phòng không được để trống.");
        Objects.requireNonNull(checkOut, "Ngày trả phòng không được để trống.");

        Instant now = Instant.now(clock);

        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .bookedBy(bookedBy)
                .roomType(roomType)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .adultCount(adults)
                .childCount(children)
                .status(BookingStatus.PENDING)
                .expiredAt(now.plus(PAYMENT_EXPIRE_MINUTES, ChronoUnit.MINUTES))
                .build();

        booking.recordCreated(bookedBy);

        return booking;
    }
}
