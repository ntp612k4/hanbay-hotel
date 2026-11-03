package com.tpanh.hotel.addon.service.model;

import java.time.Instant;
import java.util.UUID;

public record AddOnManagementResult(
        UUID id,
        String code,
        String name,
        String description,
        long price,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
