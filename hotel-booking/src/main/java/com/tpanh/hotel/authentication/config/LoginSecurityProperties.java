package com.tpanh.hotel.authentication.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.login")
public record LoginSecurityProperties(
        @Positive int ipLimit,
        @Positive long ipWindowSeconds,
        @Positive int accountFailureLimit,
        @Positive long accountWindowSeconds) {}
