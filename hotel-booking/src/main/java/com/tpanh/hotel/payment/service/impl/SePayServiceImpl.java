package com.tpanh.hotel.payment.service.impl;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.payment.config.SePayProperties;
import com.tpanh.hotel.payment.dto.result.SePayQrInfo;
import com.tpanh.hotel.payment.service.SePayService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class SePayServiceImpl implements SePayService {
    private final SePayProperties properties;

    @Override
    public SePayQrInfo generateQr(long amount, String paymentCode) {
        if (amount < 0) {
            throw new RequestValidationException("amount", "Số tiền thanh toán không được âm");
        }

        String qrUrl = UriComponentsBuilder.fromUriString(properties.qrBaseUrl())
                .queryParam("acc", properties.accountNumber())
                .queryParam("bank", properties.bankCode())
                .queryParam("amount", amount)
                .queryParam("des", paymentCode)
                .queryParam("template", "compact")
                .queryParam("download", "false")
                .queryParam("showinfo", "true")
                .build()
                .encode()
                .toUriString();

        return new SePayQrInfo(
                properties.bankCode(), properties.accountNumber(), properties.accountName(), paymentCode, qrUrl);
    }

    @Override
    public boolean verifyApiKey(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return false;
        }

        String expected = "Apikey " + properties.webhookApiKey();

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), authorizationHeader.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
