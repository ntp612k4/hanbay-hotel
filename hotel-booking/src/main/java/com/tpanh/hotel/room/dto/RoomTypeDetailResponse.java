package com.tpanh.hotel.room.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RoomTypeDetailResponse(
        UUID id,
        String name,
        String shortDescription,
        String description,
        BigDecimal basePrice,
        int maxGuests,
        int capacityAdults,
        int capacityChildren,
        List<String> images,
        List<AmenityDto> amenities) {}
