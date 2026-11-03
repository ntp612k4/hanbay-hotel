package com.tpanh.hotel.booking.dto.response;

import com.tpanh.hotel.booking.domain.BookingStatus;
import java.time.LocalDate;
import java.util.UUID;

public record TodayDepartureResponse(
        UUID bookingId,
        String bookingCode,
        String guestName,
        String guestPhone,
        String roomTypeName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int adults,
        int children,
        BookingStatus status,
        long totalAmount,
        long paidAmount,
        long outstandingAmount) {}
