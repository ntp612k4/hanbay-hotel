package com.tpanh.hotel.booking.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidBookingDateException extends DomainException {
    public InvalidBookingDateException(String message) {
        super("BOOKING_001", message, HttpStatus.BAD_REQUEST);
    }
}
