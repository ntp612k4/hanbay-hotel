package com.tpanh.hotel.reporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.reporting.dto.request.DashboardReportRequest;
import com.tpanh.hotel.reporting.dto.result.BookingMetricResult;
import com.tpanh.hotel.reporting.dto.result.DailyBookingMetricResult;
import com.tpanh.hotel.reporting.dto.result.DailyOccupancyMetricResult;
import com.tpanh.hotel.reporting.dto.result.DailyRevenueMetricResult;
import com.tpanh.hotel.reporting.dto.result.OccupancyMetricResult;
import com.tpanh.hotel.reporting.dto.result.RevenueMetricResult;
import com.tpanh.hotel.reporting.repository.DashboardMetricRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardReportServiceImplTest {

    private static final LocalDate FROM = LocalDate.of(2026, 8, 1);
    private static final LocalDate TO = LocalDate.of(2026, 8, 4);
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Instant FROM_INSTANT = FROM.atStartOfDay(HOTEL_ZONE).toInstant();
    private static final Instant TO_INSTANT = TO.atStartOfDay(HOTEL_ZONE).toInstant();

    @Mock
    private DashboardMetricRepository repository;

    private DashboardReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DashboardReportServiceImpl(repository);
    }

    @Test
    void assemblesDashboardAndCalculatesDerivedMetrics() {
        when(repository.getRevenue(FROM_INSTANT, TO_INSTANT, "VND"))
                .thenReturn(new RevenueMetricResult(1_200_000, 200_000, 1_000_000));
        when(repository.getOccupancy(FROM, TO)).thenReturn(new OccupancyMetricResult(5, 6));
        when(repository.getBookingMetrics(FROM_INSTANT, TO_INSTANT))
                .thenReturn(new BookingMetricResult(3, 0, 1, 0, 1, 1, 0, 1_000_001));
        when(repository.getDailyRevenue(FROM, TO, FROM_INSTANT, TO_INSTANT, "VND"))
                .thenReturn(List.of(
                        new DailyRevenueMetricResult(FROM, 1_200_000, 200_000, 1_000_000),
                        new DailyRevenueMetricResult(FROM.plusDays(1), 0, 0, 0),
                        new DailyRevenueMetricResult(FROM.plusDays(2), 0, 0, 0)));
        when(repository.getDailyOccupancy(FROM, TO))
                .thenReturn(List.of(
                        new DailyOccupancyMetricResult(FROM, 1, 2),
                        new DailyOccupancyMetricResult(FROM.plusDays(1), 2, 2),
                        new DailyOccupancyMetricResult(FROM.plusDays(2), 2, 2)));
        when(repository.getDailyBookingMetrics(FROM, TO, FROM_INSTANT, TO_INSTANT))
                .thenReturn(List.of(
                        new DailyBookingMetricResult(FROM, 3),
                        new DailyBookingMetricResult(FROM.plusDays(1), 0),
                        new DailyBookingMetricResult(FROM.plusDays(2), 0)));

        var result = service.getDashboardReport(new DashboardReportRequest(FROM, TO, null));

        assertThat(result.period().currency()).isEqualTo("VND");
        assertThat(result.revenue().netRevenue()).isEqualTo(1_000_000);
        assertThat(result.occupancy().occupancyRate()).isEqualByComparingTo("83.33");
        assertThat(result.bookings().averageBookingValue()).isEqualTo(333_334);
        assertThat(result.bookings().cancellationRate()).isEqualByComparingTo("33.33");
        assertThat(result.revenueTrend()).hasSize(3);
        assertThat(result.occupancyTrend())
                .extracting(item -> item.occupancyRate())
                .containsExactly(new BigDecimal("50.00"), new BigDecimal("100.00"), new BigDecimal("100.00"));
        assertThat(result.bookingTrend()).hasSize(3);
    }

    @Test
    void returnsZeroDerivedMetricsWhenThereIsNoInventoryOrBooking() {
        when(repository.getRevenue(FROM_INSTANT, TO_INSTANT, "VND")).thenReturn(new RevenueMetricResult(0, 0, 0));
        when(repository.getOccupancy(FROM, TO)).thenReturn(new OccupancyMetricResult(0, 0));
        when(repository.getBookingMetrics(FROM_INSTANT, TO_INSTANT))
                .thenReturn(new BookingMetricResult(0, 0, 0, 0, 0, 0, 0, 0));
        when(repository.getDailyRevenue(FROM, TO, FROM_INSTANT, TO_INSTANT, "VND"))
                .thenReturn(List.of());
        when(repository.getDailyOccupancy(FROM, TO)).thenReturn(List.of());
        when(repository.getDailyBookingMetrics(FROM, TO, FROM_INSTANT, TO_INSTANT))
                .thenReturn(List.of());

        var result = service.getDashboardReport(new DashboardReportRequest(FROM, TO, "VND"));

        assertThat(result.occupancy().occupancyRate()).isEqualByComparingTo("0.00");
        assertThat(result.bookings().averageBookingValue()).isZero();
        assertThat(result.bookings().cancellationRate()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsNonIncreasingDateRangeBeforeQueryingDatabase() {
        var request = new DashboardReportRequest(FROM, FROM, "VND");

        assertThatThrownBy(() -> service.getDashboardReport(request))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("Ngày kết thúc phải sau ngày bắt đầu");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsDateRangeLongerThan366Days() {
        var request = new DashboardReportRequest(FROM, FROM.plusDays(367), "VND");

        assertThatThrownBy(() -> service.getDashboardReport(request))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("Khoảng báo cáo không được vượt quá 366 ngày");
        verifyNoInteractions(repository);
    }
}
