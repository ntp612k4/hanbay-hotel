package com.tpanh.hotel.pricing.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class PricingValidationException extends DomainException {
    public PricingValidationException(String message) {
        super("PRICING_VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
    }
}
