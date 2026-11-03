package com.tpanh.hotel.authentication.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "VerifyResetTokenRequest", description = "Yêu cầu kiểm tra mã đặt lại mật khẩu")
public record VerifyResetTokenRequest(
        @NotBlank(message = "Mã đặt lại mật khẩu không được để trống") @Size(min = 32, max = 512, message = "Mã đặt lại mật khẩu không hợp lệ") @Schema(example = "base64url-token")
                String token) {}
