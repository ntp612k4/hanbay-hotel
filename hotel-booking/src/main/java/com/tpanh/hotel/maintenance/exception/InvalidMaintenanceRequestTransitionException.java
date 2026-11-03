package com.tpanh.hotel.maintenance.exception;

import com.tpanh.hotel.common.exception.DomainException;
import com.tpanh.hotel.maintenance.domain.MaintenanceRequestStatus;
import org.springframework.http.HttpStatus;

public class InvalidMaintenanceRequestTransitionException extends DomainException {

    public InvalidMaintenanceRequestTransitionException(
            MaintenanceRequestStatus currentStatus, MaintenanceRequestStatus requestedStatus) {
        super(
                "MAINTENANCE_REQUEST_STATUS_CONFLICT",
                "Không thể chuyển yêu cầu bảo trì từ " + label(currentStatus) + " sang " + label(requestedStatus),
                HttpStatus.CONFLICT);
    }

    private static String label(MaintenanceRequestStatus status) {
        return switch (status) {
            case PENDING -> "chờ xử lý";
            case IN_PROGRESS -> "đang xử lý";
            case COMPLETED -> "hoàn thành";
        };
    }
}
