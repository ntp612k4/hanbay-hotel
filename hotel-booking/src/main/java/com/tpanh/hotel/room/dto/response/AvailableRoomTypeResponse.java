package com.tpanh.hotel.room.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AvailableRoomTypeResponse(
        UUID id,
        String code,
        String name,
        String description,
        long basePrice,
        int maxGuests,
        int adultCapacity,
        int childCapacity,
        String bedType,
        BigDecimal areaM2,
        String imageUrl,
        long availableRooms,
        int totalNights,
        long roomSubtotal,
        long roomDiscount,
        long roomTotal,
        List<NightlyPriceResponse> nightlyPrices) {}
