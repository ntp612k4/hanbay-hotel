package com.tpanh.hotel.booking.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class StayModificationException extends DomainException {
    public StayModificationException(String code, String message, HttpStatus status) {
        super(code, message, status);
    }

    public static StayModificationException conflict(String code, String message) {
        return new StayModificationException(code, message, HttpStatus.CONFLICT);
    }

    public static StayModificationException badRequest(String code, String message) {
        return new StayModificationException(code, message, HttpStatus.BAD_REQUEST);
    }
}
