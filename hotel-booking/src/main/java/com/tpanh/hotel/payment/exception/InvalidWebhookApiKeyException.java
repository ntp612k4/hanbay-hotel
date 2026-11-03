package com.tpanh.hotel.payment.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidWebhookApiKeyException extends DomainException {

    public InvalidWebhookApiKeyException() {
        super("INVALID_WEBHOOK_API_KEY", "API key webhook không hợp lệ", HttpStatus.UNAUTHORIZED);
    }
}
