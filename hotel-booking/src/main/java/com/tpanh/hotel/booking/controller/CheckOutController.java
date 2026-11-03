package com.tpanh.hotel.booking.controller;

import com.tpanh.hotel.booking.dto.request.CheckOutGuestRequest;
import com.tpanh.hotel.booking.dto.response.CheckOutGuestResponse;
import com.tpanh.hotel.booking.service.CheckOutService;
import com.tpanh.hotel.common.response.ApiErrorResponse;
import com.tpanh.hotel.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/bookings")
@RequiredArgsConstructor
@Tag(name = "Front Desk", description = "Nghiệp vụ tại quầy lễ tân")
public class CheckOutController {

    private final CheckOutService checkOutService;

    @PatchMapping("/{bookingId}/check-out")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'MANAGER', 'ADMIN')")
    @Operation(
            summary = "Check-out khách",
            description =
                    "Khóa booking và phòng vật lý đã gán, kiểm tra công nợ rồi cập nhật booking CHECKED_OUT và phòng DIRTY trong cùng transaction")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Check-out khách thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Booking ID hoặc body không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Người gọi không có vai trò RECEPTIONIST, MANAGER hoặc ADMIN",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy booking",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Booking, phòng hoặc công nợ không đủ điều kiện check-out",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Lỗi hệ thống ngoài dự kiến",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ApiResponse<CheckOutGuestResponse> checkOutGuest(
            @PathVariable UUID bookingId, @RequestBody(required = false) @Valid CheckOutGuestRequest request) {
        return ApiResponse.success("Trả phòng thành công", checkOutService.checkOut(bookingId, request));
    }
}
