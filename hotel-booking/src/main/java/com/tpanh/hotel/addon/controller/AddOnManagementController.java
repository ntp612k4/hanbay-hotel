package com.tpanh.hotel.addon.controller;

import com.tpanh.hotel.addon.dto.request.CreateAddOnRequest;
import com.tpanh.hotel.addon.dto.request.UpdateAddOnRequest;
import com.tpanh.hotel.addon.dto.request.UpdateAddOnStatusRequest;
import com.tpanh.hotel.addon.dto.response.AddOnManagementResponse;
import com.tpanh.hotel.addon.mapper.AddOnMapper;
import com.tpanh.hotel.addon.service.AddOnManagementService;
import com.tpanh.hotel.addon.service.model.AddOnManagementResult;
import com.tpanh.hotel.addon.service.model.AddOnSearchCriteria;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.common.response.PageResponse;
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
@RequestMapping("${api.prefix}/management/add-ons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@Tag(name = "Quản lý dịch vụ bổ sung", description = "API quản lý danh mục dịch vụ bổ sung")
public class AddOnManagementController {

    private final AddOnManagementService addOnManagementService;
    private final AddOnMapper addOnMapper;

    @GetMapping
    @Operation(summary = "Tra cứu danh mục dịch vụ bổ sung")
    public ApiResponse<PageResponse<AddOnManagementResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "name,asc") String sort) {
        String[] sortParts = parseSort(sort);
        PageResponse<AddOnManagementResult> result = addOnManagementService.findAll(
                new AddOnSearchCriteria(page, size, search, active, sortParts[0], sortParts[1]));
        return ApiResponse.success("Lấy danh sách dịch vụ bổ sung thành công", mapPage(result));
    }

    @PostMapping
    @Operation(summary = "Tạo dịch vụ bổ sung")
    public ResponseEntity<ApiResponse<AddOnManagementResponse>> create(@Valid @RequestBody CreateAddOnRequest request) {
        AddOnManagementResponse response =
                addOnMapper.toManagementResponse(addOnManagementService.create(request.toCommand()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.success("Tạo dịch vụ bổ sung thành công", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết dịch vụ bổ sung")
    public ApiResponse<AddOnManagementResponse> findById(@PathVariable UUID id) {
        return ApiResponse.success(
                "Lấy chi tiết dịch vụ bổ sung thành công",
                addOnMapper.toManagementResponse(addOnManagementService.findById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin và giá dịch vụ bổ sung")
    public ApiResponse<AddOnManagementResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateAddOnRequest request) {
        return ApiResponse.success(
                "Cập nhật dịch vụ bổ sung thành công",
                addOnMapper.toManagementResponse(addOnManagementService.update(id, request.toCommand())));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Kích hoạt hoặc ngừng kích hoạt dịch vụ bổ sung")
    public ApiResponse<AddOnManagementResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateAddOnStatusRequest request) {
        return ApiResponse.success(
                "Cập nhật trạng thái dịch vụ bổ sung thành công",
                addOnMapper.toManagementResponse(addOnManagementService.updateStatus(id, request.toCommand())));
    }

    private String[] parseSort(String sort) {
        String[] parts = sort == null ? new String[0] : sort.trim().split(",", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new RequestValidationException("sort", "Sắp xếp phải có định dạng field,asc hoặc field,desc");
        }
        return new String[] {parts[0].trim(), parts[1].trim()};
    }

    private PageResponse<AddOnManagementResponse> mapPage(PageResponse<AddOnManagementResult> source) {
        return new PageResponse<>(
                source.content().stream().map(addOnMapper::toManagementResponse).toList(),
                source.page(),
                source.size(),
                source.totalElements(),
                source.totalPages(),
                source.first(),
                source.last());
    }
}
