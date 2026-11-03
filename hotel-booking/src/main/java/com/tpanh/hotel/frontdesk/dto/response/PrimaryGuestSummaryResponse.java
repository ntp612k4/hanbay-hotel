package com.tpanh.hotel.frontdesk.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thông tin khách chính của booking")
public record PrimaryGuestSummaryResponse(
        @Schema(example = "Nguyễn Văn A") String fullName,
        @Schema(example = "0901234567", nullable = true) String phone,
        @Schema(example = "guest@example.com", nullable = true) String email) {}
