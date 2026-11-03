package com.tpanh.hotel.booking.controller;

import com.tpanh.hotel.booking.dto.response.TodayDepartureResponse;
import com.tpanh.hotel.booking.service.TodayDepartureService;
import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/front-desk/departures")
@RequiredArgsConstructor
@Tag(name = "Lễ tân - khách trả phòng", description = "Nghiệp vụ khách trả phòng dành cho nhân viên được phân quyền")
public class TodayDepartureController {

    private final TodayDepartureService todayDepartureService;

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Xem danh sách khách dự kiến trả phòng hôm nay")
    public ApiResponse<PageResponse<TodayDepartureResponse>> getTodayDepartures(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                "Lấy danh sách khách trả phòng hôm nay thành công",
                todayDepartureService.getTodayDepartures(page, size));
    }
}
