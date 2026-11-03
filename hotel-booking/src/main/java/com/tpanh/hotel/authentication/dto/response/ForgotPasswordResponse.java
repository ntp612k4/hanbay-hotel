package com.tpanh.hotel.authentication.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ForgotPasswordResponse", description = "Kết quả tiếp nhận yêu cầu quên mật khẩu")
public record ForgotPasswordResponse(
        @Schema(example = "Nếu email tồn tại, hướng dẫn đặt lại mật khẩu sẽ được gửi.") String status) {}
