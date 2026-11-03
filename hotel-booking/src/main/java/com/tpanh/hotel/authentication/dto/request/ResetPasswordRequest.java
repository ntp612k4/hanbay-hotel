package com.tpanh.hotel.authentication.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ResetPasswordRequest", description = "Yêu cầu đặt lại mật khẩu bằng mã xác thực")
public record ResetPasswordRequest(
        @NotBlank(message = "Mã đặt lại mật khẩu không được để trống") @Size(min = 32, max = 512, message = "Mã đặt lại mật khẩu không hợp lệ") @Schema(example = "base64url-token")
                String token,
        @JsonAlias("password")
                @NotBlank(message = "Mật khẩu không được để trống") @Size(min = 8, max = 72, message = "Mật khẩu phải có từ 8 đến 72 ký tự") @Schema(example = "Password123")
                String newPassword,
        @NotBlank(message = "Xác nhận mật khẩu không được để trống") @Size(min = 8, max = 72, message = "Xác nhận mật khẩu phải có từ 8 đến 72 ký tự") @Schema(example = "Password123")
                String confirmPassword) {}
