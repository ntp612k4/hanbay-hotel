package com.tpanh.hotel.ai.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai.policy-rag")
public record PolicyRagProperties(
        boolean ragEnabled,
        @Min(1) @Max(10) int topK,
        @DecimalMin("0.0") @DecimalMax("1.0") double similarityThreshold,
        @Min(1) @Max(120) int rateLimitPerMinute,
        @Min(1) int embeddingDimensions) {}
