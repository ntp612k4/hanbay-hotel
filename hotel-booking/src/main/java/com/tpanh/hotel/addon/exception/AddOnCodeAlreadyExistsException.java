package com.tpanh.hotel.addon.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class AddOnCodeAlreadyExistsException extends DomainException {

    public AddOnCodeAlreadyExistsException() {
        super("ADD_ON_CODE_ALREADY_EXISTS", "Mã dịch vụ bổ sung đã tồn tại", HttpStatus.CONFLICT);
    }
}
