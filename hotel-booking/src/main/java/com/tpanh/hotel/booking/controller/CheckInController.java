package com.tpanh.hotel.booking.controller;

import com.tpanh.hotel.booking.dto.request.CheckInGuestRequest;
import com.tpanh.hotel.booking.dto.response.AvailablePhysicalRoomResponse;
import com.tpanh.hotel.booking.dto.response.CheckInGuestResponse;
import com.tpanh.hotel.booking.service.CheckInService;
import com.tpanh.hotel.common.response.ApiErrorResponse;
import com.tpanh.hotel.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/bookings")
@RequiredArgsConstructor
@Tag(name = "Front Desk", description = "Nghiệp vụ tại quầy lễ tân")
public class CheckInController {

    private final CheckInService checkInService;

    @GetMapping("/{bookingCode}/available-rooms")
    @PreAuthorize("hasRole('RECEPTIONIST')")
    @Operation(
            summary = "Lấy phòng vật lý khả dụng cho booking",
            description = "Trả về các phòng AVAILABLE thuộc hạng phòng của booking đã xác nhận trong ngày check-in")
    public ApiResponse<List<AvailablePhysicalRoomResponse>> getAvailableRooms(@PathVariable String bookingCode) {
        return ApiResponse.success(
                "Lấy danh sách phòng khả dụng thành công", checkInService.getAvailableRooms(bookingCode));
    }

    @PostMapping("/{bookingCode}/check-in")
    @PreAuthorize("hasRole('RECEPTIONIST')")
    @Operation(
            summary = "Gán phòng vật lý và check-in khách",
            description =
                    "Khóa booking và phòng để kiểm tra trạng thái, hạng phòng rồi cập nhật trong cùng transaction")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Gán phòng và check-in thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Booking code, room ID hoặc body không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Người gọi không có vai trò RECEPTIONIST",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy booking hoặc phòng",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Booking, ngày check-in, hạng phòng hoặc trạng thái phòng không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ApiResponse<CheckInGuestResponse> checkInGuest(
            @PathVariable String bookingCode, @RequestBody @Valid CheckInGuestRequest request) {
        return ApiResponse.success("Check-in thành công", checkInService.checkIn(bookingCode, request));
    }
}
