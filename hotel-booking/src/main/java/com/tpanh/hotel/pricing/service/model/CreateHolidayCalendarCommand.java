package com.tpanh.hotel.pricing.service.model;

import java.time.LocalDate;

public record CreateHolidayCalendarCommand(String name, LocalDate startDate, LocalDate endDate) {}
