package com.tpanh.hotel.booking.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class CapacityExceededException extends DomainException {
    public CapacityExceededException(String message) {
        super("BOOKING_002", message, HttpStatus.BAD_REQUEST);
    }
}
