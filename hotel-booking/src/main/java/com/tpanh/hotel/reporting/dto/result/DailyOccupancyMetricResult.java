package com.tpanh.hotel.reporting.dto.result;

import java.time.LocalDate;

public record DailyOccupancyMetricResult(LocalDate date, long occupiedRoomNights, long availableRoomNights) {}
