package com.tpanh.hotel.authentication.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ResetPasswordResponse", description = "Kết quả đặt lại mật khẩu")
public record ResetPasswordResponse(@Schema(example = "PASSWORD_RESET") String status) {}
