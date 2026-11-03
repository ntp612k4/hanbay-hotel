package com.tpanh.hotel.payment.domain;

import com.tpanh.hotel.common.domain.BaseEntity;
import com.tpanh.hotel.payment.exception.InvalidRefundStateException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "payment_refunds",
        indexes = {
            @Index(name = "idx_payment_refunds_payment_id", columnList = "payment_id"),
            @Index(name = "idx_payment_refunds_status_requested_at", columnList = "status, requested_at")
        })
public class PaymentRefund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PaymentRefundStatus status = PaymentRefundStatus.PENDING;

    @Column(length = 500)
    private String reason;

    @Column(name = "provider_refund_id", length = 150)
    private String providerRefundId;

    @Column(name = "provider_response_code", length = 50)
    private String providerResponseCode;

    @Column(name = "provider_message", length = 255)
    private String providerMessage;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120)
    private String idempotencyKey;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "refund_method", length = 30)
    private RefundMethod refundMethod = RefundMethod.BANK_TRANSFER;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_holder_name", length = 150)
    private String accountHolderName;

    @Column(name = "account_number", length = 34)
    private String accountNumber;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    void assignPayment(Payment payment) {
        this.payment = payment;
    }

    public void markSuccessful(String bankReference, Instant refundedAt) {
        if (status != PaymentRefundStatus.PENDING) {
            throw new InvalidRefundStateException("Chỉ yêu cầu hoàn tiền đang chờ mới có thể xác nhận");
        }
        this.status = PaymentRefundStatus.SUCCESS;
        this.providerRefundId = bankReference;
        this.refundedAt = refundedAt;
    }
}
