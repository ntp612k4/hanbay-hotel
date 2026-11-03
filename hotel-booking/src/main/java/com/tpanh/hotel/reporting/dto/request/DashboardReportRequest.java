package com.tpanh.hotel.reporting.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record DashboardReportRequest(
        @NotNull(message = "Ngày bắt đầu không được để trống") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate from,
        @NotNull(message = "Ngày kết thúc không được để trống") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate to,
        @Pattern(regexp = "[A-Z]{3}", message = "Đơn vị tiền tệ phải gồm 3 chữ cái viết hoa") String currency) {

    public DashboardReportRequest {
        if (currency == null || currency.isBlank()) {
            currency = "VND";
        }
    }
}
