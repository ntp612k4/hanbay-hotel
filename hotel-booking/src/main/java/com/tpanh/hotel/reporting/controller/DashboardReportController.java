package com.tpanh.hotel.reporting.controller;

import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.reporting.dto.request.DashboardReportRequest;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse;
import com.tpanh.hotel.reporting.service.DashboardReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/reports")
@RequiredArgsConstructor
@Tag(name = "Báo cáo quản lý", description = "Dashboard doanh thu, công suất và booking")
public class DashboardReportController {

    private final DashboardReportService dashboardReportService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Lấy báo cáo tổng quan hoạt động khách sạn")
    public ApiResponse<DashboardReportResponse> getDashboardReport(
            @ParameterObject @ModelAttribute @Valid DashboardReportRequest request) {
        return ApiResponse.success(
                "Lấy báo cáo dashboard thành công", dashboardReportService.getDashboardReport(request));
    }
}
