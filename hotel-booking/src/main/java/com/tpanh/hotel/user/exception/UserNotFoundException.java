package com.tpanh.hotel.user.exception;

import com.tpanh.hotel.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends DomainException {

    public UserNotFoundException(String message) {
        super("USER_001", message, HttpStatus.NOT_FOUND);
    }

    public UserNotFoundException(UUID userId) {
        this("Không tìm thấy người dùng: " + userId);
    }
}
