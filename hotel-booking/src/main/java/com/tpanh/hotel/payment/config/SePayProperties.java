package com.tpanh.hotel.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.sepay")
public record SePayProperties(
        String bankCode, String accountNumber, String accountName, String qrBaseUrl, String webhookApiKey) {}
