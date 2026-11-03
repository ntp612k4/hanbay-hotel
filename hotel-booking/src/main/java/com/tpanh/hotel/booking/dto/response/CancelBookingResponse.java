package com.tpanh.hotel.booking.dto.response;

import com.tpanh.hotel.booking.domain.BookingStatus;
import java.time.Instant;
import java.util.UUID;

public record CancelBookingResponse(
        UUID bookingId,
        String bookingCode,
        BookingStatus status,
        Instant cancelledAt,
        RefundStatus refundStatus,
        int refundPercent,
        long refundAmount,
        String currency) {}
