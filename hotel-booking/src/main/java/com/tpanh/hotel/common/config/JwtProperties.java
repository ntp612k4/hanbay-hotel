package com.tpanh.hotel.common.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank @Size(min = 32, message = "Khóa bí mật JWT phải có ít nhất 32 ký tự") String secret,
        @Min(60) long accessTokenExpirationSeconds,
        @Min(60) long refreshTokenExpirationSeconds) {}
