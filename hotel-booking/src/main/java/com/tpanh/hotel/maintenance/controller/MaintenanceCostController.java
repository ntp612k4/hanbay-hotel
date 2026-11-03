package com.tpanh.hotel.maintenance.controller;

import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.maintenance.dto.request.RecordMaintenanceCostRequest;
import com.tpanh.hotel.maintenance.dto.response.MaintenanceCostResponse;
import com.tpanh.hotel.maintenance.service.MaintenanceCostService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/maintenance-requests/{id}/cost")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class MaintenanceCostController {
    private final MaintenanceCostService service;

    @GetMapping
    public ApiResponse<MaintenanceCostResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(service.get(id));
    }

    @GetMapping("/history")
    public ApiResponse<List<MaintenanceCostResponse>> history(@PathVariable UUID id) {
        return ApiResponse.success(service.history(id));
    }

    @PutMapping
    public ApiResponse<MaintenanceCostResponse> record(
            @PathVariable UUID id, @Valid @RequestBody RecordMaintenanceCostRequest r) {
        return ApiResponse.success(service.record(id, r));
    }
}
