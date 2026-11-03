package com.tpanh.hotel.common.validation;

import com.tpanh.hotel.common.exception.RequestValidationException;

public final class PaginationValidator {

    private PaginationValidator() {}

    public static void validate(int page, int size, int maxPageSize) {
        if (page < 0) {
            throw new RequestValidationException("page", "Chỉ số trang phải lớn hơn hoặc bằng 0");
        }
        if (size < 1 || size > maxPageSize) {
            throw new RequestValidationException("size", "Số phần tử mỗi trang phải từ 1 đến " + maxPageSize);
        }
    }
}
