package com.tpanh.hotel.room.dto.request;

import com.tpanh.hotel.room.domain.RoomTypeStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record RoomTypeWriteRequest(
        @NotBlank @Pattern(regexp = "[A-Z0-9_-]{1,50}") String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 10000) String description,
        @Size(max = 500) String shortDescription,
        @Min(0) long basePrice,
        @Min(1) @Max(100) int maxGuests,
        @Min(1) @Max(100) int capacityAdults,
        @Min(0) @Max(100) int capacityChildren,
        @Size(max = 100) String bedType,
        @DecimalMin("0.01") @DecimalMax("9999.99") BigDecimal areaM2,
        @NotNull RoomTypeStatus status,
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 1000) String> images,
        @NotNull @Size(max = 100) Set<UUID> amenityIds) {}
