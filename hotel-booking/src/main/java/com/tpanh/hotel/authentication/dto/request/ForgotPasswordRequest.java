package com.tpanh.hotel.authentication.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

@Schema(name = "ForgotPasswordRequest", description = "Yêu cầu gửi hướng dẫn đặt lại mật khẩu")
public record ForgotPasswordRequest(
        @NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng") @Size(max = 255, message = "Email không được vượt quá 255 ký tự") @Schema(example = "guest@example.com")
                String email) {

    public ForgotPasswordRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
