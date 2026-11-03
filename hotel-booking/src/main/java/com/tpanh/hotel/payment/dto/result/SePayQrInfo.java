package com.tpanh.hotel.payment.dto.result;

public record SePayQrInfo(
        String bankCode, String accountNumber, String accountName, String transferContent, String qrUrl) {}
