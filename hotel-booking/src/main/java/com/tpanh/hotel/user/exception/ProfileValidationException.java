package com.tpanh.hotel.user.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public final class ProfileValidationException extends UserException {

    public static final String ERROR_CODE = "VALIDATION_FAILED";

    private final Map<String, String> errors;

    public ProfileValidationException(Map<String, String> errors) {
        super(ERROR_CODE, "Dữ liệu yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST);
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
