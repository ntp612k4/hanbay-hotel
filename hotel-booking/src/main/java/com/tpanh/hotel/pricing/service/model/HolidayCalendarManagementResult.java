package com.tpanh.hotel.pricing.service.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HolidayCalendarManagementResult(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
