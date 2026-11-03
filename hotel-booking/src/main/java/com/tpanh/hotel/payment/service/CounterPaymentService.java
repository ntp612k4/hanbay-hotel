package com.tpanh.hotel.payment.service;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.payment.domain.Payment;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import com.tpanh.hotel.payment.domain.PaymentTransaction;
import com.tpanh.hotel.payment.domain.PaymentTransactionStatus;
import com.tpanh.hotel.payment.domain.PaymentTransactionType;
import com.tpanh.hotel.payment.repository.PaymentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records cash collected by staff inside the transaction holding the booking lock. */
@Service
@RequiredArgsConstructor
public class CounterPaymentService {
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void settleAtCheckout(Booking booking, long amount, UUID staffId) {
        Instant now = clock.instant();
        paymentRepository
                .findAllByBookingIdAndStatusOrderByCreatedAtDesc(booking.getId(), PaymentStatus.PENDING)
                .forEach(payment -> payment.cancelPending(now));
        if (amount == 0) {
            return;
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Số tiền thu tại quầy không được âm");
        }
        Payment payment = Payment.builder()
                .booking(booking)
                .paymentCode("CASH-" + UUID.randomUUID())
                .idempotencyKey("checkout-cash:" + booking.getId())
                .amount(amount)
                .expiresAt(now)
                .build();
        payment.markSuccessful(now);
        payment.addTransaction(PaymentTransaction.builder()
                .type(PaymentTransactionType.PAYMENT)
                .status(PaymentTransactionStatus.SUCCESS)
                .amount(amount)
                .transactionRef(payment.getPaymentCode())
                .providerMessage("Thu tiền mặt tại quầy khi trả phòng")
                .rawPayload(Map.of("method", "CASH", "collectedBy", staffId.toString()))
                .occurredAt(now)
                .receivedAt(now)
                .build());
        paymentRepository.save(payment);
        booking.recordPayment(amount);
    }
}
