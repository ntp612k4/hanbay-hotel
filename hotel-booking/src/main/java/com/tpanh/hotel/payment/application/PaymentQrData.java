package com.tpanh.hotel.payment.application;

public record PaymentQrData(
        String bankCode, String accountNumber, String accountName, String transferContent, String qrUrl) {}
