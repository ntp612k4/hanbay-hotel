package com.tpanh.hotel.reporting.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardReportResponse(
        Period period,
        RevenueSummary revenue,
        OccupancySummary occupancy,
        BookingSummary bookings,
        List<DailyRevenue> revenueTrend,
        List<DailyOccupancy> occupancyTrend,
        List<DailyBookings> bookingTrend,
        MaintenanceSummary maintenance) {
    public DashboardReportResponse(
            Period period,
            RevenueSummary revenue,
            OccupancySummary occupancy,
            BookingSummary bookings,
            List<DailyRevenue> revenueTrend,
            List<DailyOccupancy> occupancyTrend,
            List<DailyBookings> bookingTrend) {
        this(period, revenue, occupancy, bookings, revenueTrend, occupancyTrend, bookingTrend, null);
    }

    public record MaintenanceSummary(
            long recordedExpense,
            long cashAfterMaintenance,
            long completedWithoutCost,
            List<DailyMaintenanceExpense> trend) {}

    public record DailyMaintenanceExpense(LocalDate date, long amount) {}

    public record Period(LocalDate from, LocalDate to, String currency) {}

    public record RevenueSummary(long grossRevenue, long refundedAmount, long netRevenue) {}

    public record OccupancySummary(long occupiedRoomNights, long availableRoomNights, BigDecimal occupancyRate) {}

    public record BookingSummary(
            long totalBookings,
            long pendingBookings,
            long confirmedBookings,
            long checkedInBookings,
            long checkedOutBookings,
            long cancelledBookings,
            long expiredBookings,
            long totalBookingValue,
            long averageBookingValue,
            BigDecimal cancellationRate) {}

    public record DailyRevenue(LocalDate date, long grossRevenue, long refundedAmount, long netRevenue) {}

    public record DailyOccupancy(
            LocalDate date, long occupiedRoomNights, long availableRoomNights, BigDecimal occupancyRate) {}

    public record DailyBookings(LocalDate date, long totalBookings) {}
}
