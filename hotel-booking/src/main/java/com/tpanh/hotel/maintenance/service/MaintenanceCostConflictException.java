package com.tpanh.hotel.maintenance.service;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class MaintenanceCostConflictException extends DomainException {
    public MaintenanceCostConflictException() {
        super(
                "MAINTENANCE_COST_CONFLICT",
                "Chi phí vừa được cập nhật. Vui lòng tải lại trước khi sửa.",
                HttpStatus.CONFLICT);
    }
}
