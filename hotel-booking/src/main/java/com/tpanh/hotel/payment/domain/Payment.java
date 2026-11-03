package com.tpanh.hotel.payment.domain;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.common.domain.BaseEntity;
import com.tpanh.hotel.payment.exception.InvalidPaymentStateException;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.type.SqlTypes;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "payments",
        indexes = {
            @Index(name = "idx_payments_booking_id", columnList = "booking_id"),
            @Index(name = "idx_payments_status_created_at", columnList = "status, created_at"),
            @Index(name = "idx_payments_status_expires_at", columnList = "status, expires_at")
        })
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @NaturalId
    @EqualsAndHashCode.Include
    @Column(name = "payment_code", nullable = false, unique = true, length = 80)
    private String paymentCode;

    @Column(nullable = false)
    private long amount;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "idempotency_key", unique = true, length = 120)
    private String idempotencyKey;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Builder.Default
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentTransaction> transactions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentRefund> refunds = new ArrayList<>();

    public void addTransaction(PaymentTransaction transaction) {
        Objects.requireNonNull(transaction, "Giao dịch thanh toán không được để trống");
        transactions.add(transaction);
        transaction.assignPayment(this);
    }

    public void addRefund(PaymentRefund refund) {
        Objects.requireNonNull(refund, "Yêu cầu hoàn tiền không được để trống");
        refunds.add(refund);
        refund.assignPayment(this);
    }

    public void cancelPending(Instant cancelledAt) {
        Objects.requireNonNull(cancelledAt, "Thời gian hủy không được để trống");
        if (status != PaymentStatus.PENDING) {
            throw InvalidPaymentStateException.notPending("hủy");
        }
        status = PaymentStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }

    public void completeRefund() {
        if (status != PaymentStatus.SUCCESS
                && status != PaymentStatus.PARTIALLY_REFUNDED
                && status != PaymentStatus.REFUNDED) {
            throw InvalidPaymentStateException.refundNotAllowed();
        }
        long receivedAmount = transactions.stream()
                .filter(transaction -> transaction.getType() == PaymentTransactionType.PAYMENT)
                .filter(transaction -> transaction.getStatus() == PaymentTransactionStatus.SUCCESS)
                .mapToLong(PaymentTransaction::getAmount)
                .sum();
        long refundedAmount = transactions.stream()
                .filter(transaction -> transaction.getType() == PaymentTransactionType.REFUND)
                .filter(transaction -> transaction.getStatus() == PaymentTransactionStatus.SUCCESS)
                .mapToLong(PaymentTransaction::getAmount)
                .sum();
        long retainedAmount = receivedAmount - refundedAmount;
        if (retainedAmount >= amount) {
            status = PaymentStatus.SUCCESS;
        } else if (retainedAmount <= 0) {
            status = PaymentStatus.REFUNDED;
        } else {
            status = PaymentStatus.PARTIALLY_REFUNDED;
        }
    }

    public boolean isPendingAndActive(Instant currentTime) {
        Objects.requireNonNull(currentTime, "Thời điểm hiện tại không được để trống");
        return status == PaymentStatus.PENDING && expiresAt != null && expiresAt.isAfter(currentTime);
    }

    public boolean isPendingAndExpired(Instant currentTime) {
        Objects.requireNonNull(currentTime, "Thời điểm hiện tại không được để trống");
        return status == PaymentStatus.PENDING && expiresAt != null && !expiresAt.isAfter(currentTime);
    }

    public void markFailed(Instant failedAt) {
        Objects.requireNonNull(failedAt, "Thời gian thất bại không được để trống");
        if (status != PaymentStatus.PENDING) {
            return;
        }

        status = PaymentStatus.FAILED;
        this.failedAt = failedAt;
    }

    public void markExpired(Instant currentTime) {
        Objects.requireNonNull(currentTime, "Thời điểm hiện tại không được để trống");
        if (status != PaymentStatus.PENDING) {
            return;
        }
        if (expiresAt == null || expiresAt.isAfter(currentTime)) {
            throw InvalidPaymentStateException.notExpired();
        }

        status = PaymentStatus.EXPIRED;
    }

    public void markSuccessful(Instant paidAt) {
        Objects.requireNonNull(paidAt, "Thời gian thanh toán không được để trống");
        if (status != PaymentStatus.PENDING) {
            throw InvalidPaymentStateException.notPending("hoàn tất");
        }
        status = PaymentStatus.SUCCESS;
        this.paidAt = paidAt;
    }

    public void reconcileSuccessfulLatePayment(Instant paidAt) {
        Objects.requireNonNull(paidAt, "Thời gian thanh toán không được để trống");
        if (status != PaymentStatus.CANCELLED && status != PaymentStatus.FAILED && status != PaymentStatus.EXPIRED) {
            throw InvalidPaymentStateException.reconciliationNotAllowed();
        }
        status = PaymentStatus.SUCCESS;
        this.paidAt = paidAt;
    }
}
