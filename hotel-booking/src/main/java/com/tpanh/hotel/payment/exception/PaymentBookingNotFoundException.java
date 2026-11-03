package com.tpanh.hotel.payment.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class PaymentBookingNotFoundException extends DomainException {
    public PaymentBookingNotFoundException(String message) {
        super("PAYMENT_BOOKING_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
