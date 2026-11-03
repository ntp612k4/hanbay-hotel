package com.tpanh.hotel.payment.service.impl;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.validation.PaginationValidator;
import com.tpanh.hotel.payment.domain.Payment;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import com.tpanh.hotel.payment.domain.PaymentWebhookEvent;
import com.tpanh.hotel.payment.domain.WebhookProcessingStatus;
import com.tpanh.hotel.payment.dto.response.MonitoredPaymentResponse;
import com.tpanh.hotel.payment.dto.response.MonitoredWebhookEventResponse;
import com.tpanh.hotel.payment.dto.response.PaymentMonitoringSummaryResponse;
import com.tpanh.hotel.payment.dto.response.WebhookEventDetailResponse;
import com.tpanh.hotel.payment.exception.WebhookEventNotFoundException;
import com.tpanh.hotel.payment.repository.PaymentRepository;
import com.tpanh.hotel.payment.repository.PaymentWebhookEventRepository;
import com.tpanh.hotel.payment.service.PaymentMonitoringService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentMonitoringServiceImpl implements PaymentMonitoringService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;

    @Override
    public PaymentMonitoringSummaryResponse getSummary() {
        return new PaymentMonitoringSummaryResponse(
                paymentRepository.count(),
                paymentRepository.countByStatus(PaymentStatus.SUCCESS),
                webhookEventRepository.count(),
                webhookEventRepository.countByProcessingStatus(WebhookProcessingStatus.FAILED));
    }

    @Override
    public PageResponse<MonitoredPaymentResponse> getPayments(
            int page, int size, String keyword, PaymentStatus status) {
        PaginationValidator.validate(page, size, MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String normalizedKeyword = normalizeKeyword(keyword);
        Page<Payment> payments = status == null
                ? paymentRepository.findForMonitoring(normalizedKeyword, pageable)
                : paymentRepository.findForMonitoringByStatus(normalizedKeyword, status, pageable);
        Page<MonitoredPaymentResponse> result = payments.map(this::toPaymentResponse);
        return PageResponse.from(result);
    }

    @Override
    public PageResponse<MonitoredWebhookEventResponse> getWebhookEvents(
            int page, int size, String keyword, WebhookProcessingStatus status) {
        PaginationValidator.validate(page, size, MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        String normalizedKeyword = normalizeKeyword(keyword);
        Page<PaymentWebhookEvent> events = status == null
                ? webhookEventRepository.findForMonitoring(normalizedKeyword, pageable)
                : webhookEventRepository.findForMonitoringByStatus(normalizedKeyword, status, pageable);
        Page<MonitoredWebhookEventResponse> result = events.map(this::toWebhookResponse);
        return PageResponse.from(result);
    }

    @Override
    public WebhookEventDetailResponse getWebhookEvent(UUID eventId) {
        PaymentWebhookEvent event =
                webhookEventRepository.findById(eventId).orElseThrow(WebhookEventNotFoundException::new);
        return new WebhookEventDetailResponse(
                event.getId(),
                event.getEventId(),
                event.getEventType(),
                event.getPayment() == null ? null : event.getPayment().getPaymentCode(),
                event.getDeduplicationKey(),
                event.getDeduplicationStrategy(),
                event.isSignatureValid(),
                event.getProcessingStatus(),
                event.getProcessingAttempts(),
                event.getProcessingStartedAt(),
                event.getProcessedAt(),
                event.getLastError(),
                event.getProviderResponseStatusCode(),
                event.getReceivedAt(),
                event.getSafeHeaders(),
                event.getPayload());
    }

    private MonitoredPaymentResponse toPaymentResponse(Payment payment) {
        return new MonitoredPaymentResponse(
                payment.getId(),
                payment.getBooking().getBookingCode(),
                payment.getPaymentCode(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getPaidAt());
    }

    private MonitoredWebhookEventResponse toWebhookResponse(PaymentWebhookEvent event) {
        Map<String, Object> payload = event.getPayload();
        return new MonitoredWebhookEventResponse(
                event.getId(),
                event.getEventId(),
                stringValue(payload, "gateway"),
                longValue(payload, "transferAmount"),
                stringValue(payload, "referenceCode"),
                event.getPayment() == null ? null : event.getPayment().getPaymentCode(),
                event.getProcessingStatus(),
                event.getProcessingAttempts(),
                event.getReceivedAt(),
                event.getLastError());
    }

    private String stringValue(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? null : value.toString();
    }

    private long longValue(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }
}
