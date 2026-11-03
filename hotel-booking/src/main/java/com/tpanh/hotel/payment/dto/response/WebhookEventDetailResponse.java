package com.tpanh.hotel.payment.dto.response;

import com.tpanh.hotel.payment.domain.WebhookDeduplicationStrategy;
import com.tpanh.hotel.payment.domain.WebhookProcessingStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WebhookEventDetailResponse(
        UUID id,
        String eventId,
        String eventType,
        String paymentCode,
        String deduplicationKey,
        WebhookDeduplicationStrategy deduplicationStrategy,
        boolean signatureValid,
        WebhookProcessingStatus status,
        int attempts,
        Instant processingStartedAt,
        Instant processedAt,
        String lastError,
        Integer providerResponseStatusCode,
        Instant receivedAt,
        Map<String, Object> safeHeaders,
        Map<String, Object> payload) {}
