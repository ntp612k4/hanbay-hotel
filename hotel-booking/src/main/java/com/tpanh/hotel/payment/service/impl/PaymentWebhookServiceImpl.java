package com.tpanh.hotel.payment.service.impl;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.payment.config.SePayProperties;
import com.tpanh.hotel.payment.domain.*;
import com.tpanh.hotel.payment.dto.SePayTransferType;
import com.tpanh.hotel.payment.dto.request.SePayWebhookRequest;
import com.tpanh.hotel.payment.exception.InvalidWebhookApiKeyException;
import com.tpanh.hotel.payment.repository.PaymentRepository;
import com.tpanh.hotel.payment.repository.PaymentTransactionRepository;
import com.tpanh.hotel.payment.repository.PaymentWebhookEventRepository;
import com.tpanh.hotel.payment.service.PaymentWebhookService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private static final String AUTHENTICATION_SCHEME = "Apikey ";
    private static final String DEDUPLICATION_PREFIX = "sepay:event:";
    private static final Pattern FORMATTED_PAYMENT_CODE =
            Pattern.compile("(?<![A-Za-z0-9])PAY-\\d{14}-[A-Fa-f0-9]{8,32}(?![A-Za-z0-9])");
    private static final Pattern COMPACT_PAYMENT_CODE =
            Pattern.compile("(?<![A-Za-z0-9])PAY(\\d{14})([A-Fa-f0-9]{8,32})(?![A-Za-z0-9])");
    private static final ZoneId SEPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentWebhookEventRepository webhookEventRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final SePayProperties sePayProperties;
    private final Clock clock;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public void handleSePayWebhook(String authorization, SePayWebhookRequest request) {
        verifyApiKey(authorization);

        String eventId = Long.toString(request.id());
        String deduplicationKey = DEDUPLICATION_PREFIX + eventId;
        webhookEventRepository.acquireDeduplicationLock(deduplicationKey);
        if (webhookEventRepository.existsByDeduplicationKey(deduplicationKey)
                || transactionRepository.existsByProviderTransactionId(eventId)) {
            return;
        }

        Instant now = clock.instant();
        PaymentWebhookEvent event = PaymentWebhookEvent.builder()
                .eventId(eventId)
                .eventType("bank.transfer")
                .deduplicationKey(deduplicationKey)
                .deduplicationStrategy(WebhookDeduplicationStrategy.EVENT_ID)
                .payload(toPayload(request))
                .signatureValid(true)
                .receivedAt(now)
                .build();
        webhookEventRepository.save(event);

        if (request.transferType() != SePayTransferType.IN) {
            event.markIgnored(null, "Bỏ qua giao dịch chuyển tiền ra.", now);
            return;
        }
        if (!normalized(request.accountNumber()).equals(normalized(sePayProperties.accountNumber()))) {
            event.markIgnored(null, "Tài khoản nhận tiền không thuộc HANBAY HOTEL.", now);
            return;
        }

        Optional<String> paymentCode = extractPaymentCode(request);
        if (paymentCode.isEmpty()) {
            event.markIgnored(null, "Không tìm thấy mã thanh toán trong nội dung chuyển khoản.", now);
            return;
        }

        // Read no managed booking state until its lock is held, so counter collection and
        // incoming transfers cannot overwrite each other's paid amount or booking status.
        Booking booking = paymentRepository
                .findBookingIdByPaymentCode(paymentCode.get())
                .flatMap(bookingRepository::findByIdWithAssignedRoomForUpdate)
                .orElse(null);
        if (booking == null) {
            event.markIgnored(null, "Mã thanh toán không tồn tại.", now);
            return;
        }
        Payment payment =
                paymentRepository.findByPaymentCodeForUpdate(paymentCode.get()).orElse(null);
        if (payment == null) {
            event.markIgnored(null, "Mã thanh toán không tồn tại.", now);
            return;
        }

        Instant occurredAt = request.transactionDate().atZone(SEPAY_ZONE).toInstant();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .providerTransactionId(eventId)
                .transactionRef(request.referenceCode())
                .type(PaymentTransactionType.PAYMENT)
                .status(PaymentTransactionStatus.SUCCESS)
                .amount(request.transferAmount())
                .currency("VND")
                .providerMessage(request.description())
                .rawPayload(toPayload(request))
                .occurredAt(occurredAt)
                .receivedAt(now)
                .build();
        payment.addTransaction(transaction);
        transactionRepository.save(transaction);

        long receivedAmount = transactionRepository.sumSuccessfulPaymentAmount(payment.getId());
        if (canSettleAdditionalBalance(payment, booking)) {
            booking.recordPayment(request.transferAmount());
            if (receivedAmount >= payment.getAmount()) {
                payment.markSuccessful(occurredAt);
                if (receivedAmount > payment.getAmount()) {
                    createOverpaymentRefund(payment, receivedAmount - payment.getAmount(), eventId, now);
                }
            }
        } else if (!canConfirmBooking(payment, booking, now)) {
            preparePaymentForRefund(payment, occurredAt);
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                createOverpaymentRefund(payment, request.transferAmount(), eventId, now);
            } else {
                createLatePaymentRefund(payment, request.transferAmount(), eventId, now);
            }
        } else {
            booking.recordPayment(request.transferAmount());
            if (receivedAmount >= payment.getAmount()) {
                payment.markSuccessful(occurredAt);
                booking.confirmAutomatically(now);
                if (receivedAmount > payment.getAmount()) {
                    createOverpaymentRefund(payment, receivedAmount - payment.getAmount(), eventId, now);
                }
            }
        }
        event.markProcessed(payment, now);
    }

    private boolean canConfirmBooking(Payment payment, Booking booking, Instant now) {
        return payment.getStatus() == PaymentStatus.PENDING
                && booking.getStatus() == BookingStatus.PENDING
                && booking.getExpiredAt() != null
                && booking.getExpiredAt().isAfter(now);
    }

    private boolean canSettleAdditionalBalance(Payment payment, Booking booking) {
        return payment.getStatus() == PaymentStatus.PENDING
                && payment.getIdempotencyKey() != null
                && payment.getIdempotencyKey().startsWith("additional-balance:")
                && (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.CHECKED_IN);
    }

    private void preparePaymentForRefund(Payment payment, Instant occurredAt) {
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.markSuccessful(occurredAt);
        } else if (payment.getStatus() == PaymentStatus.CANCELLED
                || payment.getStatus() == PaymentStatus.FAILED
                || payment.getStatus() == PaymentStatus.EXPIRED) {
            payment.reconcileSuccessfulLatePayment(occurredAt);
        }
    }

    private void createOverpaymentRefund(Payment payment, long amount, String eventId, Instant now) {
        createRefund(
                payment,
                amount,
                "Hoàn phần tiền thanh toán vượt quá tổng tiền booking",
                refundIdempotencyKey("overpayment", payment, eventId),
                now);
    }

    private void createLatePaymentRefund(Payment payment, long receivedAmount, String eventId, Instant now) {
        createRefund(
                payment,
                receivedAmount,
                "Thanh toán được ghi nhận sau khi booking không còn hiệu lực để xác nhận",
                refundIdempotencyKey("late-payment", payment, eventId),
                now);
    }

    private String refundIdempotencyKey(String reason, Payment payment, String eventId) {
        return reason + ":" + payment.getId() + ":" + eventId;
    }

    private void createRefund(Payment payment, long amount, String reason, String idempotencyKey, Instant now) {
        payment.addRefund(PaymentRefund.builder()
                .amount(BigDecimal.valueOf(amount))
                .currency("VND")
                .status(PaymentRefundStatus.PENDING)
                .reason(reason)
                .idempotencyKey(idempotencyKey)
                .requestedAt(now)
                .build());
        payment.addTransaction(PaymentTransaction.builder()
                .type(PaymentTransactionType.REFUND)
                .status(PaymentTransactionStatus.PENDING)
                .amount(amount)
                .currency("VND")
                .providerMessage(reason)
                .receivedAt(now)
                .build());
    }

    private Optional<String> extractPaymentCode(SePayWebhookRequest request) {
        for (String candidate : new String[] {request.code(), request.content(), request.description()}) {
            if (candidate == null) {
                continue;
            }
            Matcher formattedMatcher = FORMATTED_PAYMENT_CODE.matcher(candidate);
            if (formattedMatcher.find()) {
                return Optional.of(formattedMatcher.group().toUpperCase());
            }
            Matcher compactMatcher = COMPACT_PAYMENT_CODE.matcher(candidate);
            if (compactMatcher.find()) {
                return Optional.of(("PAY-" + compactMatcher.group(1) + "-" + compactMatcher.group(2)).toUpperCase());
            }
        }
        return Optional.empty();
    }

    private String normalized(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toUpperCase();
    }

    private void verifyApiKey(String authorization) {
        String configuredApiKey = sePayProperties.webhookApiKey();
        String expectedAuthorization = AUTHENTICATION_SCHEME + configuredApiKey;
        if (configuredApiKey == null
                || configuredApiKey.isBlank()
                || authorization == null
                || !MessageDigest.isEqual(
                        expectedAuthorization.getBytes(StandardCharsets.UTF_8),
                        authorization.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidWebhookApiKeyException();
        }
    }

    private Map<String, Object> toPayload(SePayWebhookRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", request.id());
        payload.put("gateway", request.gateway());
        payload.put("transactionDate", request.transactionDate().toString());
        payload.put("accountNumber", request.accountNumber());
        payload.put("subAccount", request.subAccount());
        payload.put("code", request.code());
        payload.put("content", request.content());
        payload.put("transferType", request.transferType().value());
        payload.put("transferAmount", request.transferAmount());
        payload.put("accumulated", request.accumulated());
        payload.put("description", request.description());
        payload.put("referenceCode", request.referenceCode());
        return payload;
    }
}
