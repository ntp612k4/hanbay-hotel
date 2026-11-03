package com.tpanh.hotel.payment.dto.response;

import com.tpanh.hotel.payment.domain.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record MonitoredPaymentResponse(
        UUID id,
        String bookingCode,
        String paymentCode,
        long amount,
        String currency,
        PaymentStatus status,
        Instant createdAt,
        Instant paidAt) {}
