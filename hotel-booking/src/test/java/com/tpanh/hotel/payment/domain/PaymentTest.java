package com.tpanh.hotel.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private static final Instant NOW = Instant.parse("2026-08-16T08:00:00Z");

    @Test
    void remainsSuccessfulAfterRefundingOnlyOverpayment() {
        Payment payment = Payment.builder()
                .amount(1_500_000L)
                .status(PaymentStatus.SUCCESS)
                .build();
        payment.addTransaction(
                transaction(PaymentTransactionType.PAYMENT, PaymentTransactionStatus.SUCCESS, 1_700_000L));
        PaymentTransaction refund =
                transaction(PaymentTransactionType.REFUND, PaymentTransactionStatus.PENDING, 200_000L);
        payment.addTransaction(refund);

        refund.markSuccessful("REF-OVERPAYMENT", NOW);
        payment.completeRefund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void becomesPartiallyRefundedWhenRefundReducesRetainedAmountBelowCharge() {
        Payment payment = Payment.builder()
                .amount(1_500_000L)
                .status(PaymentStatus.SUCCESS)
                .build();
        payment.addTransaction(
                transaction(PaymentTransactionType.PAYMENT, PaymentTransactionStatus.SUCCESS, 1_500_000L));
        PaymentTransaction refund =
                transaction(PaymentTransactionType.REFUND, PaymentTransactionStatus.PENDING, 500_000L);
        payment.addTransaction(refund);

        refund.markSuccessful("REF-PARTIAL", NOW);
        payment.completeRefund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
    }

    @Test
    void completesRemainingRefundAfterPaymentBecomesPartiallyRefunded() {
        Payment payment = Payment.builder()
                .amount(1_500_000L)
                .status(PaymentStatus.SUCCESS)
                .build();
        payment.addTransaction(
                transaction(PaymentTransactionType.PAYMENT, PaymentTransactionStatus.SUCCESS, 1_700_000L));
        PaymentTransaction cancellationRefund =
                transaction(PaymentTransactionType.REFUND, PaymentTransactionStatus.PENDING, 1_500_000L);
        PaymentTransaction overpaymentRefund =
                transaction(PaymentTransactionType.REFUND, PaymentTransactionStatus.PENDING, 200_000L);
        payment.addTransaction(cancellationRefund);
        payment.addTransaction(overpaymentRefund);

        cancellationRefund.markSuccessful("REF-CANCELLATION", NOW);
        payment.completeRefund();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);

        overpaymentRefund.markSuccessful("REF-OVERPAYMENT", NOW.plusSeconds(1));
        payment.completeRefund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    private PaymentTransaction transaction(PaymentTransactionType type, PaymentTransactionStatus status, long amount) {
        return PaymentTransaction.builder()
                .type(type)
                .status(status)
                .amount(amount)
                .receivedAt(NOW)
                .build();
    }
}
