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
        name = "payment_webhook_events",
        indexes = {
            @Index(
                    name = "idx_payment_webhook_events_status_received_at",
                    columnList = "processing_status, received_at"),
            @Index(name = "idx_payment_webhook_events_payment_id", columnList = "payment_id"),
            @Index(name = "idx_payment_webhook_events_event_id", columnList = "event_id")
        })
public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "event_id", nullable = false, length = 120)
    private String eventId;

    @Column(name = "event_type", length = 80)
    private String eventType;

    @Column(name = "deduplication_key", nullable = false, unique = true, length = 255)
    private String deduplicationKey;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "deduplication_strategy", nullable = false)
    private WebhookDeduplicationStrategy deduplicationStrategy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "safe_headers", columnDefinition = "jsonb")
    private Map<String, Object> safeHeaders;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "processing_status", nullable = false)
    private WebhookProcessingStatus processingStatus = WebhookProcessingStatus.RECEIVED;

    @Builder.Default
    @Column(name = "processing_attempts", nullable = false)
    private int processingAttempts = 0;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "provider_response_status_code")
    private Integer providerResponseStatusCode;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public void assignPayment(Payment payment) {
        this.payment = payment;
    }

    public void markProcessed(Payment payment, Instant processedAt) {
        this.payment = payment;
        this.processingStatus = WebhookProcessingStatus.PROCESSED;
        this.processingAttempts++;
        this.processingStartedAt = processedAt;
        this.processedAt = processedAt;
        this.lastError = null;
    }

    public void markIgnored(Payment payment, String reason, Instant processedAt) {
        this.payment = payment;
        this.processingStatus = WebhookProcessingStatus.IGNORED;
        this.processingAttempts++;
        this.processingStartedAt = processedAt;
        this.processedAt = processedAt;
        this.lastError = reason;
    }
}
