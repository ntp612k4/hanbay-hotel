package com.tpanh.hotel.payment.service;

import com.tpanh.hotel.payment.dto.result.SePayQrInfo;

public interface SePayService {
    SePayQrInfo generateQr(long amount, String paymentCode);

    boolean verifyApiKey(String authorizationHeader);

    String sha256(String value);
}
