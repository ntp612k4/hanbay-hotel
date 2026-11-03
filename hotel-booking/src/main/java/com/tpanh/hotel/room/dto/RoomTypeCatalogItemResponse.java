package com.tpanh.hotel.room.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomTypeCatalogItemResponse(
        UUID id,
        String name,
        String description,
        int capacityAdults,
        int capacityChildren,
        BigDecimal basePrice,
        String thumbnailUrl) {}
