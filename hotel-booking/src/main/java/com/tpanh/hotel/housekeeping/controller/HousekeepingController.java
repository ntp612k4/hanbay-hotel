package com.tpanh.hotel.housekeeping.controller;

import com.tpanh.hotel.common.response.ApiErrorResponse;
import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.housekeeping.dto.response.DirtyRoomResponse;
import com.tpanh.hotel.housekeeping.service.HousekeepingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/housekeeping")
@RequiredArgsConstructor
@Tag(name = "Housekeeping", description = "API vận hành buồng phòng")
public class HousekeepingController {

    private final HousekeepingService housekeepingService;

    @GetMapping("/dirty-rooms")
    @PreAuthorize("hasRole('HOUSEKEEPER')")
    @Operation(summary = "Xem danh sách phòng cần dọn", description = "Chỉ trả phòng có trạng thái DIRTY")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy danh sách phòng cần dọn thành công"),
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
                description = "Tài khoản không có quyền Housekeeping",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Lỗi hệ thống ngoài dự kiến",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ApiResponse<PageResponse<DirtyRoomResponse>> getDirtyRooms(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                "Lấy danh sách phòng cần dọn thành công", housekeepingService.getDirtyRooms(page, size));
    }

    @GetMapping("/rooms")
    @PreAuthorize("hasRole('HOUSEKEEPER')")
    @Operation(summary = "Xem các phòng cần xử lý", description = "Trả phòng DIRTY và CLEANING")
    public ApiResponse<PageResponse<DirtyRoomResponse>> getRoomsRequiringCleaning(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                "Lấy danh sách phòng cần xử lý thành công", housekeepingService.getRoomsRequiringCleaning(page, size));
    }

    @PatchMapping("/rooms/{roomId}/start-cleaning")
    @PreAuthorize("hasRole('HOUSEKEEPER')")
    @Operation(summary = "Bắt đầu dọn phòng", description = "Chuyển phòng DIRTY sang CLEANING")
    public ApiResponse<DirtyRoomResponse> startCleaning(@PathVariable UUID roomId) {
        return ApiResponse.success("Đã bắt đầu dọn phòng", housekeepingService.startCleaning(roomId));
    }

    @PatchMapping("/rooms/{roomId}/complete-cleaning")
    @PreAuthorize("hasRole('HOUSEKEEPER')")
    @Operation(summary = "Hoàn tất dọn phòng", description = "Chuyển phòng CLEANING sang AVAILABLE")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Đã hoàn tất dọn phòng"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "roomId không đúng định dạng UUID",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Thiếu hoặc sai access token",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Tài khoản không có quyền Housekeeping",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy phòng",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Phòng không còn ở trạng thái CLEANING",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Lỗi hệ thống ngoài dự kiến",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ApiResponse<DirtyRoomResponse> completeCleaning(@PathVariable UUID roomId) {
        return ApiResponse.success("Đã hoàn tất dọn phòng", housekeepingService.completeCleaning(roomId));
    }
}
