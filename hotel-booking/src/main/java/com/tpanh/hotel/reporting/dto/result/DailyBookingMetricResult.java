package com.tpanh.hotel.reporting.dto.result;

import java.time.LocalDate;

public record DailyBookingMetricResult(LocalDate date, long totalBookings) {}
