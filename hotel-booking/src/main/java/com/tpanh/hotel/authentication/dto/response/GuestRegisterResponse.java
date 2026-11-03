package com.tpanh.hotel.authentication.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "GuestRegisterResponse", description = "Tài khoản khách vừa được tạo")
public record GuestRegisterResponse(
        @Schema(description = "Mã người dùng") UUID id,
        @Schema(example = "Nguyễn Văn A") String fullName,
        @Schema(example = "guest@example.com") String email,
        @Schema(example = "0901234567") String phone,
        @Schema(example = "ACTIVE") String status) {}
