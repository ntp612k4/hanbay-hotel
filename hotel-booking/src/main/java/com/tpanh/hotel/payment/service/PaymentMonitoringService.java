package com.tpanh.hotel.payment.service;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import com.tpanh.hotel.payment.domain.WebhookProcessingStatus;
import com.tpanh.hotel.payment.dto.response.MonitoredPaymentResponse;
import com.tpanh.hotel.payment.dto.response.MonitoredWebhookEventResponse;
import com.tpanh.hotel.payment.dto.response.PaymentMonitoringSummaryResponse;
import com.tpanh.hotel.payment.dto.response.WebhookEventDetailResponse;
import java.util.UUID;

public interface PaymentMonitoringService {

    PaymentMonitoringSummaryResponse getSummary();

    PageResponse<MonitoredPaymentResponse> getPayments(int page, int size, String keyword, PaymentStatus status);

    PageResponse<MonitoredWebhookEventResponse> getWebhookEvents(
            int page, int size, String keyword, WebhookProcessingStatus status);

    WebhookEventDetailResponse getWebhookEvent(UUID eventId);
}
