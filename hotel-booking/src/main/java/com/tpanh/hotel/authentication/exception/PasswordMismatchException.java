package com.tpanh.hotel.authentication.exception;

import com.tpanh.hotel.common.exception.DomainException;

public class PasswordMismatchException extends DomainException {

    public PasswordMismatchException() {
        super(
                "PASSWORD_MISMATCH",
                "Mật khẩu và xác nhận mật khẩu không khớp",
                org.springframework.http.HttpStatus.BAD_REQUEST);
    }
}
