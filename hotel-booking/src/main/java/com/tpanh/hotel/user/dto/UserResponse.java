package com.tpanh.hotel.user.dto;

import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Thông tin cá nhân của người dùng hiện tại")
public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        UserStatus status,
        List<String> roles,
        Instant createdAt,
        Instant updatedAt) {}
