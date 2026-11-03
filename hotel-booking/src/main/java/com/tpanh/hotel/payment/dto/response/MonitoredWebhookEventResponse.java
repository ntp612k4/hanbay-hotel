package com.tpanh.hotel.payment.dto.response;

import com.tpanh.hotel.payment.domain.WebhookProcessingStatus;
import java.time.Instant;
import java.util.UUID;

public record MonitoredWebhookEventResponse(
        UUID id,
        String eventId,
        String gateway,
        long transferAmount,
        String transactionReference,
        String paymentCode,
        WebhookProcessingStatus status,
        int attempts,
        Instant receivedAt,
        String lastError) {}
