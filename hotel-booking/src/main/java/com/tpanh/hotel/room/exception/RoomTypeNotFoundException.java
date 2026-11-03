package com.tpanh.hotel.room.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class RoomTypeNotFoundException extends DomainException {
    public RoomTypeNotFoundException(String message) {
        super("ROOM_001", message, HttpStatus.NOT_FOUND);
    }
}
