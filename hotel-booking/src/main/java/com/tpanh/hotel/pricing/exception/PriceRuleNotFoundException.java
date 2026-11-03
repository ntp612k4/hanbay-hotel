package com.tpanh.hotel.pricing.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class PriceRuleNotFoundException extends DomainException {
    public PriceRuleNotFoundException() {
        super("PRICE_RULE_NOT_FOUND", "Khong tim thay quy tac gia", HttpStatus.NOT_FOUND);
    }
}
