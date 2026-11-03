package com.tpanh.hotel.pricing.dto.result;

import java.time.LocalDate;
import java.util.List;

public record StayPriceResult(
        RoomTypePricingResult roomType,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int numberOfNights,
        List<NightlyPriceResult> nightlyPrices,
        long totalPrice) {}
