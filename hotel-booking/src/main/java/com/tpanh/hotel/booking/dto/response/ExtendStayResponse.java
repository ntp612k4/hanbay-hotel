package com.tpanh.hotel.booking.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record ExtendStayResponse(
        UUID bookingId,
        String bookingCode,
        LocalDate previousCheckOutDate,
        LocalDate newCheckOutDate,
        long additionalNights,
        long additionalAmount,
        long newTotalAmount,
        long outstandingAmount,
        boolean paymentRequired,
        UUID retainedRoomId,
        String currency) {}
