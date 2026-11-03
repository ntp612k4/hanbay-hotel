package com.tpanh.hotel.authentication.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidPasswordResetTokenException extends DomainException {

    public InvalidPasswordResetTokenException() {
        super("AUTH_RESET_TOKEN_INVALID", "Mã đặt lại mật khẩu không hợp lệ", HttpStatus.BAD_REQUEST);
    }
}
