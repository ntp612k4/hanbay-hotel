package com.tpanh.hotel.payment.domain;

public enum WebhookDeduplicationStrategy {
    EVENT_ID,
    TRANSACTION_ID,
    BUSINESS_HASH
}
