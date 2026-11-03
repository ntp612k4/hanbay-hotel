package com.tpanh.hotel.room.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class RoomNotFoundException extends DomainException {

    public RoomNotFoundException(String message) {
        super("ROOM_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
