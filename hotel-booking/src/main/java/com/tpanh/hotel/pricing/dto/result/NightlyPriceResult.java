package com.tpanh.hotel.pricing.dto.result;

import java.time.LocalDate;

public record NightlyPriceResult(LocalDate date, long finalPrice) {}
