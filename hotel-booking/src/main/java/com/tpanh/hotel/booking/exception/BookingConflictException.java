package com.tpanh.hotel.booking.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class BookingConflictException extends DomainException {
    public BookingConflictException(String message) {
        super("BOOKING_CODE_CONFLICT", message, HttpStatus.CONFLICT);
    }
}
