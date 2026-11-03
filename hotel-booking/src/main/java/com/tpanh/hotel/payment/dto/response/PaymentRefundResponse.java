package com.tpanh.hotel.payment.dto.response;

import com.tpanh.hotel.payment.domain.PaymentRefundStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRefundResponse(
        UUID refundId,
        UUID paymentId,
        BigDecimal amount,
        String currency,
        PaymentRefundStatus status,
        String reason,
        String providerRefundId,
        Instant requestedAt,
        Instant refundedAt,
        Instant createdAt,
        Instant updatedAt) {}
