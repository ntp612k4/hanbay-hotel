package com.tpanh.hotel.user.controller;

import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.user.dto.UpdateUserRequest;
import com.tpanh.hotel.user.dto.UserResponse;
import com.tpanh.hotel.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix:/api/v1}/users/me")
@Tag(name = "Thông tin cá nhân", description = "Xem và cập nhật thông tin cá nhân của tài khoản đang đăng nhập")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Lấy thông tin cá nhân của người dùng hiện tại")
    public ApiResponse<UserResponse> getCurrentProfile() {
        return ApiResponse.success("Lấy thông tin cá nhân thành công", userService.getCurrentProfile());
    }

    @PatchMapping
    @Operation(summary = "Cập nhật thông tin cá nhân của người dùng hiện tại")
    public ApiResponse<UserResponse> updateCurrentProfile(@RequestBody UpdateUserRequest request) {
        return ApiResponse.success(
                "Cập nhật thông tin cá nhân thành công", userService.updateCurrentProfile(request.toCommand()));
    }
}
