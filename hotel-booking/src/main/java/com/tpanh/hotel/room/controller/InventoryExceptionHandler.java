package com.tpanh.hotel.room.controller;

import com.tpanh.hotel.common.response.ApiErrorResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(basePackages = "com.tpanh.hotel.room.controller")
@Order(0)
public class InventoryExceptionHandler {
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> conflict(
            DataIntegrityViolationException ex, jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(409)
                .body(new ApiErrorResponse(
                        "INVENTORY_CONFLICT",
                        "Mã đã tồn tại hoặc bản ghi đang được sử dụng. Không thể thực hiện thay đổi.",
                        request.getRequestURI(),
                        Instant.now(),
                        Map.of()));
    }
}
