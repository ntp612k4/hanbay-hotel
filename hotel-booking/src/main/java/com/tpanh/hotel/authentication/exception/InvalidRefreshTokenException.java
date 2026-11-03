package com.tpanh.hotel.authentication.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException() {
        super("AUTH_INVALID_REFRESH_TOKEN", "Refresh token không hợp lệ", HttpStatus.UNAUTHORIZED);
    }
}
