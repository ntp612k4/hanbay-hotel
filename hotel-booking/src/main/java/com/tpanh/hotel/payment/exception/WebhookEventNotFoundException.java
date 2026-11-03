package com.tpanh.hotel.payment.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class WebhookEventNotFoundException extends DomainException {

    public WebhookEventNotFoundException() {
        super("WEBHOOK_EVENT_NOT_FOUND", "Không tìm thấy webhook event", HttpStatus.NOT_FOUND);
    }
}
