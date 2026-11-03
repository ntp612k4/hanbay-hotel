package com.tpanh.hotel.room.controller;

import com.tpanh.hotel.common.response.*;
import com.tpanh.hotel.room.dto.request.RoomWriteRequest;
import com.tpanh.hotel.room.dto.response.RoomManagementResponse;
import com.tpanh.hotel.room.service.RoomManagementService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix:/api/v1}/management/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class RoomManagementController {
    private final RoomManagementService service;

    @GetMapping
    public ApiResponse<PageResponse<RoomManagementResponse>> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.list(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomManagementResponse> detail(@PathVariable UUID id) {
        return ApiResponse.success(service.detail(id));
    }

    @PostMapping
    public ApiResponse<RoomManagementResponse> create(@Valid @RequestBody RoomWriteRequest r) {
        return ApiResponse.success(service.create(r));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoomManagementResponse> update(@PathVariable UUID id, @Valid @RequestBody RoomWriteRequest r) {
        return ApiResponse.success(service.update(id, r));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.success("Đã xóa phòng", null);
    }
}
