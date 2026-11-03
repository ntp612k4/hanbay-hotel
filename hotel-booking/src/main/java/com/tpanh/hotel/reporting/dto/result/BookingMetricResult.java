package com.tpanh.hotel.reporting.dto.result;

public record BookingMetricResult(
        long totalBookings,
        long pendingBookings,
        long confirmedBookings,
        long checkedInBookings,
        long checkedOutBookings,
        long cancelledBookings,
        long expiredBookings,
        long totalBookingValue) {}
