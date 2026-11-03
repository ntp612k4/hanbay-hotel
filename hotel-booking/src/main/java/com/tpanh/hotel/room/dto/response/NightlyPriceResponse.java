package com.tpanh.hotel.room.dto.response;

import java.time.LocalDate;
import java.util.List;

public record NightlyPriceResponse(LocalDate stayDate, long price, List<NightlyPriceAdjustmentResponse> adjustments) {}
