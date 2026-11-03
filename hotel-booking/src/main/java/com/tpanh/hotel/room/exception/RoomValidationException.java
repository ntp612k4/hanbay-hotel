package com.tpanh.hotel.room.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class RoomValidationException extends DomainException {
    public RoomValidationException(String message) {
        super("ROOM_VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
    }
}
