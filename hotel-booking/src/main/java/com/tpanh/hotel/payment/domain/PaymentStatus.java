package com.tpanh.hotel.payment.domain;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED,
    REFUNDED,
    EXPIRED,
    PARTIALLY_REFUNDED
}
