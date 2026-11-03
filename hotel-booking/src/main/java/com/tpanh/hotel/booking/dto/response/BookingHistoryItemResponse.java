package com.tpanh.hotel.booking.dto.response;

import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.payment.domain.PaymentRefundStatus;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BookingHistoryItemResponse(
        UUID bookingId,
        String bookingCode,
        BookingHistoryRoomTypeResponse roomType,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int adultCount,
        int childCount,
        long totalNights,
        BigDecimal totalAmount,
        BigDecimal outstandingAmount,
        String currency,
        BookingStatus bookingStatus,
        UUID paymentId,
        PaymentStatus paymentStatus,
        PaymentRefundStatus refundStatus,
        BigDecimal refundAmount,
        Instant createdAt) {}
