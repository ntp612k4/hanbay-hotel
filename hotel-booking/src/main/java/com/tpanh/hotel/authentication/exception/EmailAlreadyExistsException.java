package com.tpanh.hotel.authentication.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends DomainException {

    public EmailAlreadyExistsException(String email) {
        super("EMAIL_ALREADY_EXISTS", "Email đã tồn tại: " + email, HttpStatus.CONFLICT);
    }
}
