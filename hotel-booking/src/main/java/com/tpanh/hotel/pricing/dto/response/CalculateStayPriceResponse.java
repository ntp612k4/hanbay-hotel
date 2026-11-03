package com.tpanh.hotel.pricing.dto.response;

import java.time.LocalDate;
import java.util.List;

public record CalculateStayPriceResponse(
        RoomTypePriceResponse roomType,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int numberOfNights,
        List<StayNightPriceResponse> nightlyPrices,
        long totalPrice) {}
