package com.tpanh.hotel.pricing.dto.result;

public record PricingResult(RoomPricingResult roomPricingResult, long addOnAmount, long totalAmount) {}
