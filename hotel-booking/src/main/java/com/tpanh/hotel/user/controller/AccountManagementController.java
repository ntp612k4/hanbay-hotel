package com.tpanh.hotel.user.controller;

import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.user.dto.*;
import com.tpanh.hotel.user.service.AccountManagementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix:/api/v1}/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Quản lý tài khoản và phân quyền")
public class AccountManagementController {
    private final AccountManagementService service;

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) UserStatus status) {
        return ApiResponse.success(service.list(page, size, search, role, status));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        return ApiResponse.success("Tạo tài khoản thành công", service.create(request));
    }

    @PatchMapping("/{id}/roles")
    public ApiResponse<UserResponse> updateRoles(
            @PathVariable UUID id, @Valid @RequestBody UpdateAccountRolesRequest request) {
        return ApiResponse.success("Cập nhật vai trò thành công", service.updateRoles(id, request.roles()));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateAccountStatusRequest request) {
        return ApiResponse.success("Cập nhật trạng thái thành công", service.updateStatus(id, request.status()));
    }
}
