package com.tpanh.hotel.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GuestInfo(
        @NotBlank(message = "Họ tên không được để trống") String fullName,
        @NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng") String email,
        @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại không đúng định dạng") String phone,
        @Schema(
                        description = "Số giấy tờ tùy thân/CCCD của khách",
                        example = "012345678901",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "Số CCCD không được để trống") String identityNumber) {}
