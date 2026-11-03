package com.tpanh.hotel.room.controller;

import com.tpanh.hotel.common.response.*;
import com.tpanh.hotel.room.domain.Amenity;
import com.tpanh.hotel.room.dto.*;
import com.tpanh.hotel.room.dto.request.RoomTypeWriteRequest;
import com.tpanh.hotel.room.service.RoomTypeManagementService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix:/api/v1}/management/room-types")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class RoomTypeManagementController {
    private final RoomTypeManagementService service;

    @GetMapping
    public ApiResponse<PageResponse<RoomTypeManagementResponse>> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.list(page, size));
    }

    @GetMapping("/amenities")
    public ApiResponse<List<Amenity>> amenities() {
        return ApiResponse.success(service.amenities());
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomTypeManagementResponse> detail(@PathVariable UUID id) {
        return ApiResponse.success(service.detail(id));
    }

    @PostMapping
    public ApiResponse<RoomTypeManagementResponse> create(@Valid @RequestBody RoomTypeWriteRequest r) {
        return ApiResponse.success(service.save(null, r));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoomTypeManagementResponse> update(
            @PathVariable UUID id, @Valid @RequestBody RoomTypeWriteRequest r) {
        return ApiResponse.success(service.save(id, r));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.success("Đã xóa loại phòng", null);
    }
}
