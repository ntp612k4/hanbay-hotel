package com.tpanh.hotel.authentication.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequest", description = "Dữ liệu đăng nhập")
public record LoginRequest(
        @NotBlank(message = "Vui lòng nhập email hoặc số điện thoại") @Size(max = 255, message = "Email hoặc số điện thoại không được vượt quá 255 ký tự") @Schema(description = "Email hoặc số điện thoại", example = "admin@aegean-suites.com")
                String identifier,
        @NotBlank(message = "Vui lòng nhập mật khẩu") @Size(min = 8, max = 72, message = "Mật khẩu phải có từ 8 đến 72 ký tự") @Schema(description = "Mật khẩu", example = "Admin@123")
                String password) {}
