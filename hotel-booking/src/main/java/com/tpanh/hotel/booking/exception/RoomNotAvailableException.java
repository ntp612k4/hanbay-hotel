package com.tpanh.hotel.booking.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class RoomNotAvailableException extends DomainException {
    public RoomNotAvailableException(String message) {
        super("BOOKING_003", message, HttpStatus.CONFLICT);
    }
}
