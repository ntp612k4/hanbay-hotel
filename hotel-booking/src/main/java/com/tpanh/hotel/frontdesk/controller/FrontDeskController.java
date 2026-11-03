package com.tpanh.hotel.frontdesk.controller;

import com.tpanh.hotel.common.response.ApiErrorResponse;
import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.frontdesk.dto.response.TodayArrivalResponse;
import com.tpanh.hotel.frontdesk.service.FrontDeskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/front-desk")
@RequiredArgsConstructor
@Tag(name = "Front Desk", description = "API vận hành quầy lễ tân")
public class FrontDeskController {

    private final FrontDeskService frontDeskService;

    @GetMapping("/arrivals/today")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'MANAGER', 'ADMIN')")
    @Operation(
            summary = "Xem danh sách khách dự kiến đến hôm nay",
            description = "Chỉ trả booking CONFIRMED có ngày nhận phòng là hôm nay theo Asia/Ho_Chi_Minh")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy danh sách khách đến hôm nay thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Tham số phân trang không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Thiếu hoặc sai access token",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Tài khoản không có quyền Front Desk",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Lỗi hệ thống ngoài dự kiến",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ApiResponse<PageResponse<TodayArrivalResponse>> getTodayArrivals(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                "Lấy danh sách khách đến hôm nay thành công", frontDeskService.getTodayArrivals(page, size));
    }
}
