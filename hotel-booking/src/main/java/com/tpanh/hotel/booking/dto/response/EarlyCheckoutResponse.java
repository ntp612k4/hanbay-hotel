package com.tpanh.hotel.booking.dto.response;

import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.payment.domain.PaymentRefundStatus;
import java.time.LocalDate;
import java.util.UUID;

public record EarlyCheckoutResponse(
        UUID bookingId,
        String bookingCode,
        BookingStatus bookingStatus,
        LocalDate originalCheckOutDate,
        LocalDate actualCheckOutDate,
        long unusedNights,
        long unusedAmount,
        int penaltyPercent,
        long penaltyAmount,
        long refundAmount,
        String currency,
        PaymentRefundStatus refundStatus,
        AssignedRoomResponse room) {}
