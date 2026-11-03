package com.tpanh.hotel.pricing.controller;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.pricing.dto.request.CreateHolidayCalendarRequest;
import com.tpanh.hotel.pricing.dto.request.UpdateHolidayCalendarRequest;
import com.tpanh.hotel.pricing.dto.request.UpdateHolidayCalendarStatusRequest;
import com.tpanh.hotel.pricing.dto.response.HolidayCalendarManagementResponse;
import com.tpanh.hotel.pricing.service.HolidayCalendarManagementService;
import com.tpanh.hotel.pricing.service.model.HolidayCalendarManagementResult;
import com.tpanh.hotel.pricing.service.model.HolidayCalendarSearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("${api.prefix}/management/holiday-calendar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@Tag(name = "Quản lý lịch ngày lễ", description = "API cấu hình ngày lễ và sự kiện dùng trong dynamic pricing")
public class HolidayCalendarManagementController {

    private final HolidayCalendarManagementService holidayCalendarManagementService;

    @GetMapping
    @Operation(summary = "Tra cứu lịch ngày lễ")
    public ApiResponse<PageResponse<HolidayCalendarManagementResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "startDate,desc") String sort) {
        String[] sortParts = parseSort(sort);
        PageResponse<HolidayCalendarManagementResult> result = holidayCalendarManagementService.findAll(
                new HolidayCalendarSearchCriteria(page, size, search, active, sortParts[0], sortParts[1]));
        return ApiResponse.success("Lấy danh sách ngày lễ thành công", mapPage(result));
    }

    @PostMapping
    @Operation(summary = "Tạo ngày lễ hoặc sự kiện")
    public ResponseEntity<ApiResponse<HolidayCalendarManagementResponse>> create(
            @Valid @RequestBody CreateHolidayCalendarRequest request) {
        HolidayCalendarManagementResponse response =
                toResponse(holidayCalendarManagementService.create(request.toCommand()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.success("Tạo ngày lễ thành công", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết ngày lễ")
    public ApiResponse<HolidayCalendarManagementResponse> findById(@PathVariable UUID id) {
        return ApiResponse.success(
                "Lấy chi tiết ngày lễ thành công", toResponse(holidayCalendarManagementService.findById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật ngày lễ")
    public ApiResponse<HolidayCalendarManagementResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateHolidayCalendarRequest request) {
        return ApiResponse.success(
                "Cập nhật ngày lễ thành công",
                toResponse(holidayCalendarManagementService.update(id, request.toCommand())));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Kích hoạt hoặc ngừng kích hoạt ngày lễ")
    public ApiResponse<HolidayCalendarManagementResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateHolidayCalendarStatusRequest request) {
        return ApiResponse.success(
                "Cập nhật trạng thái ngày lễ thành công",
                toResponse(holidayCalendarManagementService.updateStatus(id, request.toCommand())));
    }

    private String[] parseSort(String sort) {
        String[] parts = sort == null ? new String[0] : sort.trim().split(",", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new RequestValidationException("sort", "Sắp xếp phải có định dạng field,asc hoặc field,desc");
        }
        return new String[] {parts[0].trim(), parts[1].trim()};
    }

    private PageResponse<HolidayCalendarManagementResponse> mapPage(
            PageResponse<HolidayCalendarManagementResult> source) {
        return new PageResponse<>(
                source.content().stream().map(this::toResponse).toList(),
                source.page(),
                source.size(),
                source.totalElements(),
                source.totalPages(),
                source.first(),
                source.last());
    }

    private HolidayCalendarManagementResponse toResponse(HolidayCalendarManagementResult result) {
        return new HolidayCalendarManagementResponse(
                result.id(),
                result.name(),
                result.startDate(),
                result.endDate(),
                result.active(),
                result.createdAt(),
                result.updatedAt());
    }
}
