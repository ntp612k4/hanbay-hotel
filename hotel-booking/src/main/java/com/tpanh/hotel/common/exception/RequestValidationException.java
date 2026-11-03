package com.tpanh.hotel.common.exception;

import lombok.Getter;

@Getter
public class RequestValidationException extends IllegalArgumentException {

    private final String field;

    public RequestValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
}
