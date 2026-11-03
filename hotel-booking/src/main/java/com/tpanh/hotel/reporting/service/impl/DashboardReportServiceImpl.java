package com.tpanh.hotel.reporting.service.impl;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.reporting.dto.request.DashboardReportRequest;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse.BookingSummary;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse.DailyBookings;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse.DailyOccupancy;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse.DailyRevenue;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse.OccupancySummary;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse.Period;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse.RevenueSummary;
import com.tpanh.hotel.reporting.repository.DashboardMetricRepository;
import com.tpanh.hotel.reporting.service.DashboardReportService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardReportServiceImpl implements DashboardReportService {

    static final int MAX_REPORT_DAYS = 366;
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DashboardMetricRepository metricRepository;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DashboardReportResponse getDashboardReport(DashboardReportRequest request) {
        validateDateRange(request.from(), request.to());
        Instant fromInstant = request.from().atStartOfDay(HOTEL_ZONE).toInstant();
        Instant toInstant = request.to().atStartOfDay(HOTEL_ZONE).toInstant();

        var revenue = metricRepository.getRevenue(fromInstant, toInstant, request.currency());
        var occupancy = metricRepository.getOccupancy(request.from(), request.to());
        var bookings = metricRepository.getBookingMetrics(fromInstant, toInstant);

        var revenueTrend =
                metricRepository
                        .getDailyRevenue(request.from(), request.to(), fromInstant, toInstant, request.currency())
                        .stream()
                        .map(item -> new DailyRevenue(
                                item.date(), item.grossRevenue(), item.refundedAmount(), item.netRevenue()))
                        .toList();
        var occupancyTrend = metricRepository.getDailyOccupancy(request.from(), request.to()).stream()
                .map(item -> new DailyOccupancy(
                        item.date(),
                        item.occupiedRoomNights(),
                        item.availableRoomNights(),
                        percentage(item.occupiedRoomNights(), item.availableRoomNights())))
                .toList();
        var bookingTrend =
                metricRepository.getDailyBookingMetrics(request.from(), request.to(), fromInstant, toInstant).stream()
                        .map(item -> new DailyBookings(item.date(), item.totalBookings()))
                        .toList();

        var costTrend = "VND".equals(request.currency())
                ? metricRepository.getDailyMaintenanceExpense(request.from(), request.to())
                : java.util.List.<DashboardReportResponse.DailyMaintenanceExpense>of();
        long expenses = costTrend.stream().mapToLong(item -> item.amount()).sum();
        var maintenance = new DashboardReportResponse.MaintenanceSummary(
                expenses,
                revenue.netRevenue() - expenses,
                metricRepository.countCompletedMaintenanceWithoutCost(request.from(), request.to()),
                costTrend);
        return new DashboardReportResponse(
                new Period(request.from(), request.to(), request.currency()),
                new RevenueSummary(revenue.grossRevenue(), revenue.refundedAmount(), revenue.netRevenue()),
                new OccupancySummary(
                        occupancy.occupiedRoomNights(),
                        occupancy.availableRoomNights(),
                        percentage(occupancy.occupiedRoomNights(), occupancy.availableRoomNights())),
                new BookingSummary(
                        bookings.totalBookings(),
                        bookings.pendingBookings(),
                        bookings.confirmedBookings(),
                        bookings.checkedInBookings(),
                        bookings.checkedOutBookings(),
                        bookings.cancelledBookings(),
                        bookings.expiredBookings(),
                        bookings.totalBookingValue(),
                        average(bookings.totalBookingValue(), bookings.totalBookings()),
                        percentage(bookings.cancelledBookings(), bookings.totalBookings())),
                revenueTrend,
                occupancyTrend,
                bookingTrend,
                maintenance);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (!from.isBefore(to)) {
            throw new RequestValidationException("to", "Ngày kết thúc phải sau ngày bắt đầu");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_REPORT_DAYS) {
            throw new RequestValidationException("to", "Khoảng báo cáo không được vượt quá 366 ngày");
        }
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private long average(long total, long count) {
        return count == 0
                ? 0
                : BigDecimal.valueOf(total)
                        .divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP)
                        .longValueExact();
    }
}
