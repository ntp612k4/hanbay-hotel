package com.tpanh.hotel.authentication.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class PasswordResetTokenUsedException extends DomainException {

    public PasswordResetTokenUsedException() {
        super("AUTH_RESET_TOKEN_USED", "Mã đặt lại mật khẩu đã được sử dụng hoặc bị thu hồi", HttpStatus.BAD_REQUEST);
    }
}
