package com.tpanh.hotel.authentication.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.password-reset")
public record PasswordResetProperties(
        @Positive long expirationMinutes,
        @NotBlank @Pattern(regexp = "https?://.+", message = "URL đặt lại mật khẩu phải dùng HTTP hoặc HTTPS") String frontendUrl,
        @NotBlank @Email String fromAddress,
        @NotBlank @Email String supportEmail,
        @Positive long rateLimitWindowSeconds,
        @Positive int forgotRateLimit,
        @Positive int verifyRateLimit,
        @Positive int submitRateLimit,
        @Positive int retentionDays) {}
