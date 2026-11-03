package com.tpanh.hotel.pricing.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class PriceRuleConflictException extends DomainException {
    public PriceRuleConflictException() {
        super("PRICE_RULE_CONFLICT", "Xung dot du lieu khi cap nhat quy tac gia", HttpStatus.CONFLICT);
    }
}
