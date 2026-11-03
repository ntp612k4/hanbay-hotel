package com.tpanh.hotel.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.payment.domain.Payment;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import com.tpanh.hotel.payment.domain.PaymentWebhookEvent;
import com.tpanh.hotel.payment.domain.WebhookDeduplicationStrategy;
import com.tpanh.hotel.payment.domain.WebhookProcessingStatus;
import com.tpanh.hotel.payment.repository.PaymentRepository;
import com.tpanh.hotel.payment.repository.PaymentWebhookEventRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PaymentMonitoringServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentWebhookEventRepository webhookEventRepository;

    private PaymentMonitoringServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentMonitoringServiceImpl(paymentRepository, webhookEventRepository);
    }

    @Test
    void returnsMonitoringSummary() {
        when(paymentRepository.count()).thenReturn(12L);
        when(paymentRepository.countByStatus(PaymentStatus.SUCCESS)).thenReturn(8L);
        when(webhookEventRepository.count()).thenReturn(15L);
        when(webhookEventRepository.countByProcessingStatus(WebhookProcessingStatus.FAILED))
                .thenReturn(2L);

        var result = service.getSummary();

        assertThat(result.totalPayments()).isEqualTo(12);
        assertThat(result.successfulPayments()).isEqualTo(8);
        assertThat(result.totalWebhookEvents()).isEqualTo(15);
        assertThat(result.failedWebhookEvents()).isEqualTo(2);
    }

    @Test
    void mapsPaymentPageWithoutExposingEntity() {
        Booking booking = Booking.builder().bookingCode("HB-260809-001").build();
        Payment payment = Payment.builder()
                .booking(booking)
                .paymentCode("HBABC123456")
                .amount(1_500_000L)
                .expiresAt(Instant.parse("2026-08-09T10:00:00Z"))
                .status(PaymentStatus.SUCCESS)
                .build();
        payment.setCreatedAt(Instant.parse("2026-08-09T09:00:00Z"));
        when(paymentRepository.findForMonitoringByStatus(
                        org.mockito.ArgumentMatchers.eq("HBABC"),
                        org.mockito.ArgumentMatchers.eq(PaymentStatus.SUCCESS),
                        org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(payment)));

        var result = service.getPayments(0, 10, " HBABC ", PaymentStatus.SUCCESS);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).bookingCode()).isEqualTo("HB-260809-001");
        assertThat(result.content().get(0).amount()).isEqualTo(1_500_000L);
        assertThat(result.content().get(0).status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void rejectsInvalidPageSize() {
        assertThatThrownBy(() -> service.getPayments(0, 0, null, null)).isInstanceOf(RequestValidationException.class);
    }

    @Test
    void mapsWebhookListFieldsFromStoredPayload() {
        PaymentWebhookEvent event = webhookEvent();
        when(webhookEventRepository.findForMonitoringByStatus(
                        org.mockito.ArgumentMatchers.eq("987"),
                        org.mockito.ArgumentMatchers.eq(WebhookProcessingStatus.RECEIVED),
                        org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(event)));

        var result = service.getWebhookEvents(0, 10, " 987 ", WebhookProcessingStatus.RECEIVED);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).gateway()).isEqualTo("MBBank");
        assertThat(result.content().get(0).transferAmount()).isEqualTo(1_500_000L);
        assertThat(result.content().get(0).transactionReference()).isEqualTo("FT2622098765");
    }

    @Test
    void returnsWebhookDetailWithRawPayload() {
        UUID id = UUID.randomUUID();
        PaymentWebhookEvent event = webhookEvent(id);
        when(webhookEventRepository.findById(id)).thenReturn(Optional.of(event));

        var result = service.getWebhookEvent(id);

        assertThat(result.eventId()).isEqualTo("987654321");
        assertThat(result.payload()).containsEntry("content", "HBABC123456");
        assertThat(result.signatureValid()).isTrue();
    }

    private PaymentWebhookEvent webhookEvent() {
        return webhookEvent(UUID.randomUUID());
    }

    private PaymentWebhookEvent webhookEvent(UUID id) {
        return PaymentWebhookEvent.builder()
                .id(id)
                .eventId("987654321")
                .eventType("bank.transfer")
                .deduplicationKey("sepay:event:987654321")
                .deduplicationStrategy(WebhookDeduplicationStrategy.EVENT_ID)
                .signatureValid(true)
                .processingStatus(WebhookProcessingStatus.RECEIVED)
                .receivedAt(Instant.parse("2026-08-09T09:30:00Z"))
                .payload(Map.of(
                        "gateway", "MBBank",
                        "transferAmount", 1_500_000L,
                        "referenceCode", "FT2622098765",
                        "content", "HBABC123456"))
                .build();
    }
}
