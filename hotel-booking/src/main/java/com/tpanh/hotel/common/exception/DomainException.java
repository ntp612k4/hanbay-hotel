package com.tpanh.hotel.common.exception;

import org.springframework.http.HttpStatus;

public abstract class DomainException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    protected DomainException(String message) {
        this("DOMAIN_EXCEPTION", message, HttpStatus.BAD_REQUEST);
    }

    protected DomainException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
