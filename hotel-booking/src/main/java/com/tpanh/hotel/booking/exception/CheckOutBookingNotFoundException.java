package com.tpanh.hotel.booking.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class CheckOutBookingNotFoundException extends DomainException {

    public CheckOutBookingNotFoundException(String message) {
        super("BOOKING_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
