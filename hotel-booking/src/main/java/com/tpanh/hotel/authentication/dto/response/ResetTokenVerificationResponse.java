package com.tpanh.hotel.authentication.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "ResetTokenVerificationResponse", description = "Kết quả kiểm tra mã đặt lại mật khẩu")
public record ResetTokenVerificationResponse(
        @Schema(example = "RESET_TOKEN_VALID") String status,
        @Schema(example = "2026-08-10T06:15:00Z") Instant expiresAt) {}
