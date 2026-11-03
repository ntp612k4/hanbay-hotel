package com.tpanh.hotel.room.controller;

import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.room.dto.request.RoomMaintenanceCommandRequest;
import com.tpanh.hotel.room.dto.response.RoomMaintenanceResponse;
import com.tpanh.hotel.room.service.RoomMaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/rooms")
@RequiredArgsConstructor
@Tag(name = "Room Maintenance", description = "Quản lý trạng thái bảo trì phòng")
public class RoomMaintenanceController {

    private final RoomMaintenanceService roomMaintenanceService;

    @PatchMapping("/{roomNumber}/maintenance")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Xác nhận thay đổi trạng thái bảo trì", description = "Chỉ MANAGER được thực hiện")
    public ApiResponse<RoomMaintenanceResponse> changeMaintenanceStatus(
            @PathVariable String roomNumber, @RequestBody @Valid RoomMaintenanceCommandRequest request) {
        return ApiResponse.success(
                "Cập nhật trạng thái bảo trì thành công",
                roomMaintenanceService.changeMaintenanceStatus(roomNumber, request.action()));
    }
}
