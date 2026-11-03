package com.tpanh.hotel.authentication.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class PhoneAlreadyExistsException extends DomainException {

    public PhoneAlreadyExistsException(String phone) {
        super("PHONE_ALREADY_EXISTS", "Số điện thoại đã tồn tại: " + phone, HttpStatus.CONFLICT);
    }
}
