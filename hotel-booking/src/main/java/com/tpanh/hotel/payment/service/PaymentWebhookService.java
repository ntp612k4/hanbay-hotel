package com.tpanh.hotel.payment.service;

import com.tpanh.hotel.payment.dto.request.SePayWebhookRequest;

public interface PaymentWebhookService {

    void handleSePayWebhook(String authorization, SePayWebhookRequest request);
}
