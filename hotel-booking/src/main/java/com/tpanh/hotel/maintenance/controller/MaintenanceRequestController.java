package com.tpanh.hotel.maintenance.controller;

import com.tpanh.hotel.common.response.ApiErrorResponse;
import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.maintenance.dto.request.CreateMaintenanceRequestRequest;
import com.tpanh.hotel.maintenance.dto.request.UpdateMaintenanceRequestStatusRequest;
import com.tpanh.hotel.maintenance.dto.response.MaintenanceRequestListItemResponse;
import com.tpanh.hotel.maintenance.dto.response.MaintenanceRequestResponse;
import com.tpanh.hotel.maintenance.service.MaintenanceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("${api.prefix}/maintenance-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "Room Maintenance", description = "Quản lý yêu cầu bảo trì phòng vật lý")
public class MaintenanceRequestController {

    private final MaintenanceRequestService maintenanceRequestService;

    @PostMapping
    @Operation(summary = "Tạo yêu cầu bảo trì")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Dữ liệu không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy phòng",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Trạng thái phòng không cho phép bảo trì",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<MaintenanceRequestResponse>> create(
            @Valid @RequestBody CreateMaintenanceRequestRequest request) {
        MaintenanceRequestResponse response = maintenanceRequestService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.success("Tạo yêu cầu bảo trì thành công", response));
    }

    @GetMapping
    @Operation(summary = "Xem danh sách yêu cầu bảo trì")
    public ApiResponse<PageResponse<MaintenanceRequestListItemResponse>> findAll(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                "Lấy danh sách yêu cầu bảo trì thành công", maintenanceRequestService.findAll(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết yêu cầu bảo trì")
    public ApiResponse<MaintenanceRequestResponse> findById(@PathVariable UUID id) {
        return ApiResponse.success("Lấy chi tiết yêu cầu bảo trì thành công", maintenanceRequestService.findById(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái xử lý yêu cầu bảo trì")
    public ApiResponse<MaintenanceRequestResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateMaintenanceRequestStatusRequest request) {
        return ApiResponse.success(
                "Cập nhật trạng thái yêu cầu bảo trì thành công", maintenanceRequestService.updateStatus(id, request));
    }
}
