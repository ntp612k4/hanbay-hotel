package com.tpanh.hotel.addon.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class AddOnNotFoundException extends DomainException {

    public AddOnNotFoundException() {
        super("ADD_ON_NOT_FOUND", "Không tìm thấy dịch vụ bổ sung", HttpStatus.NOT_FOUND);
    }
}
