package com.tpanh.hotel.authentication.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("AUTH_INVALID_CREDENTIALS", "Email, số điện thoại hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED);
    }
}
