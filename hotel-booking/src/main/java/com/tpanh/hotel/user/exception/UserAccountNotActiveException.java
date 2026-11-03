package com.tpanh.hotel.user.exception;

import org.springframework.http.HttpStatus;

public final class UserAccountNotActiveException extends UserException {

    public static final String ERROR_CODE = "USER_002";

    public UserAccountNotActiveException() {
        super(ERROR_CODE, "Tài khoản người dùng không hoạt động", HttpStatus.FORBIDDEN);
    }
}
