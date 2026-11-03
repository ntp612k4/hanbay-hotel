package com.tpanh.hotel.payment.dto.response;

import com.tpanh.hotel.payment.domain.PaymentRefundStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundRequestResponse(
        UUID refundId,
        String bookingCode,
        String paymentCode,
        BigDecimal amount,
        String currency,
        String reason,
        PaymentRefundStatus status,
        String bankReference,
        Instant requestedAt,
        Instant refundedAt,
        String bankName,
        String accountHolderName,
        String accountNumber) {
    public RefundRequestResponse(
            UUID refundId,
            String bookingCode,
            String paymentCode,
            BigDecimal amount,
            String currency,
            String reason,
            PaymentRefundStatus status,
            String bankReference,
            Instant requestedAt,
            Instant refundedAt) {
        this(
                refundId,
                bookingCode,
                paymentCode,
                amount,
                currency,
                reason,
                status,
                bankReference,
                requestedAt,
                refundedAt,
                null,
                null,
                null);
    }
}
