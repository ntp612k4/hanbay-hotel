package com.tpanh.hotel.addon.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AddOnManagementResponse(
        UUID id,
        String code,
        String name,
        String description,
        long price,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
