package com.tpanh.hotel.booking.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class BookingNotFoundException extends DomainException {
    public BookingNotFoundException(String message) {
        super("BOOKING_404", message, HttpStatus.NOT_FOUND);
    }
}
