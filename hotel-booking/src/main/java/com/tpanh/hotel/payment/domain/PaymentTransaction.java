package com.tpanh.hotel.payment.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "payment_transactions",
        indexes = {
            @Index(name = "idx_payment_transactions_payment_received_at", columnList = "payment_id, received_at"),
            @Index(name = "idx_payment_transactions_transaction_ref", columnList = "transaction_ref"),
            @Index(name = "idx_payment_transactions_status_received_at", columnList = "status, received_at")
        })
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "provider_transaction_id", unique = true, length = 150)
    private String providerTransactionId;

    @Column(name = "transaction_ref", length = 120)
    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PaymentTransactionType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PaymentTransactionStatus status;

    @Column(nullable = false)
    private long amount;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "provider_response_code", length = 50)
    private String providerResponseCode;

    @Column(name = "provider_message", length = 255)
    private String providerMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private Map<String, Object> rawPayload;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    void assignPayment(Payment payment) {
        this.payment = payment;
    }

    public void markSuccessful(String transactionReference, Instant completedAt) {
        if (status != PaymentTransactionStatus.PENDING || type != PaymentTransactionType.REFUND) {
            throw new IllegalStateException("Chỉ giao dịch hoàn tiền đang chờ mới có thể xác nhận");
        }
        this.transactionRef = transactionReference;
        this.status = PaymentTransactionStatus.SUCCESS;
        this.occurredAt = completedAt;
    }
}
