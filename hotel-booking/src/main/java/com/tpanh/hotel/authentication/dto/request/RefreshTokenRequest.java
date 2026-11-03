package com.tpanh.hotel.authentication.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank(message = "Mã làm mới không được để trống") String refreshToken) {}
