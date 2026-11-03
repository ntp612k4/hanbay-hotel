package com.tpanh.hotel.authentication.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class PasswordResetTokenExpiredException extends DomainException {

    public PasswordResetTokenExpiredException() {
        super("AUTH_RESET_TOKEN_EXPIRED", "Mã đặt lại mật khẩu đã hết hạn", HttpStatus.BAD_REQUEST);
    }
}
