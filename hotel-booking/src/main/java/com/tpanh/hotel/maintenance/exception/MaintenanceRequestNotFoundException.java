package com.tpanh.hotel.maintenance.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class MaintenanceRequestNotFoundException extends DomainException {

    public MaintenanceRequestNotFoundException() {
        super("MAINTENANCE_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu bảo trì", HttpStatus.NOT_FOUND);
    }
}
