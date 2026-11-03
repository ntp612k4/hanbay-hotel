package com.tpanh.hotel.pricing.service.model;

import java.time.LocalDate;

public record UpdateHolidayCalendarCommand(String name, LocalDate startDate, LocalDate endDate) {}
