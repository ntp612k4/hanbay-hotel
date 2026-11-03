package com.tpanh.hotel.reporting.dto.result;

import java.time.LocalDate;

public record DailyRevenueMetricResult(LocalDate date, long grossRevenue, long refundedAmount, long netRevenue) {}
