package com.tpanh.hotel.reporting.repository;

import com.tpanh.hotel.reporting.dto.result.BookingMetricResult;
import com.tpanh.hotel.reporting.dto.result.DailyBookingMetricResult;
import com.tpanh.hotel.reporting.dto.result.DailyOccupancyMetricResult;
import com.tpanh.hotel.reporting.dto.result.DailyRevenueMetricResult;
import com.tpanh.hotel.reporting.dto.result.OccupancyMetricResult;
import com.tpanh.hotel.reporting.dto.result.RevenueMetricResult;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DashboardMetricRepository {

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final String REVENUE_AGGREGATE =
            """
            SELECT COALESCE(SUM(pt.amount) FILTER (WHERE pt.type = 'PAYMENT'), 0) AS gross_revenue,
                   COALESCE(SUM(pt.amount) FILTER (WHERE pt.type = 'REFUND'), 0) AS refunded_amount
              FROM payment_transactions pt
             WHERE pt.status = 'SUCCESS'
               AND pt.currency = :currency
               AND pt.occurred_at >= :fromInstant
               AND pt.occurred_at < :toInstant
            """;

    private static final String DAILY_REVENUE =
            """
            WITH days AS (
                SELECT generate_series(CAST(:fromDate AS date), CAST(:toDate AS date) - 1, interval '1 day')::date AS metric_date
            ), revenue AS (
                SELECT pt.occurred_at::date AS metric_date,
                       COALESCE(SUM(pt.amount) FILTER (WHERE pt.type = 'PAYMENT'), 0) AS gross_revenue,
                       COALESCE(SUM(pt.amount) FILTER (WHERE pt.type = 'REFUND'), 0) AS refunded_amount
                  FROM payment_transactions pt
                 WHERE pt.status = 'SUCCESS'
                   AND pt.currency = :currency
                   AND pt.occurred_at >= :fromInstant
                   AND pt.occurred_at < :toInstant
                 GROUP BY pt.occurred_at::date
            )
            SELECT days.metric_date,
                   COALESCE(revenue.gross_revenue, 0) AS gross_revenue,
                   COALESCE(revenue.refunded_amount, 0) AS refunded_amount
              FROM days
              LEFT JOIN revenue ON revenue.metric_date = days.metric_date
             ORDER BY days.metric_date
            """;

    private static final String OCCUPANCY_AGGREGATE =
            """
            SELECT (SELECT COUNT(*)
                      FROM booking_nights bn
                      JOIN bookings b ON b.id = bn.booking_id
                     WHERE bn.stay_date >= :fromDate
                       AND bn.stay_date < :toDate
                       AND b.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')) AS occupied_room_nights,
                   (SELECT COUNT(*) FROM rooms) * (CAST(:toDate AS date) - CAST(:fromDate AS date)) AS available_room_nights
            """;

    private static final String DAILY_OCCUPANCY =
            """
            WITH days AS (
                SELECT generate_series(CAST(:fromDate AS date), CAST(:toDate AS date) - 1, interval '1 day')::date AS metric_date
            ), occupied AS (
                SELECT bn.stay_date AS metric_date, COUNT(*) AS occupied_room_nights
                  FROM booking_nights bn
                  JOIN bookings b ON b.id = bn.booking_id
                 WHERE bn.stay_date >= :fromDate
                   AND bn.stay_date < :toDate
                   AND b.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                 GROUP BY bn.stay_date
            ), inventory AS (
                SELECT COUNT(*) AS available_rooms FROM rooms
            )
            SELECT days.metric_date,
                   COALESCE(occupied.occupied_room_nights, 0) AS occupied_room_nights,
                   inventory.available_rooms AS available_room_nights
              FROM days
              CROSS JOIN inventory
              LEFT JOIN occupied ON occupied.metric_date = days.metric_date
             ORDER BY days.metric_date
            """;

    private static final String BOOKING_AGGREGATE =
            """
            SELECT COUNT(*) AS total_bookings,
                   COUNT(*) FILTER (WHERE status = 'PENDING') AS pending_bookings,
                   COUNT(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed_bookings,
                   COUNT(*) FILTER (WHERE status = 'CHECKED_IN') AS checked_in_bookings,
                   COUNT(*) FILTER (WHERE status = 'CHECKED_OUT') AS checked_out_bookings,
                   COUNT(*) FILTER (WHERE status = 'CANCELLED') AS cancelled_bookings,
                   COUNT(*) FILTER (WHERE status = 'EXPIRED') AS expired_bookings,
                   COALESCE(SUM(total_amount), 0) AS total_booking_value
              FROM bookings
             WHERE created_at >= :fromInstant
               AND created_at < :toInstant
            """;

    private static final String DAILY_BOOKINGS =
            """
            WITH days AS (
                SELECT generate_series(CAST(:fromDate AS date), CAST(:toDate AS date) - 1, interval '1 day')::date AS metric_date
            ), booking_counts AS (
                SELECT created_at::date AS metric_date, COUNT(*) AS total_bookings
                  FROM bookings
                 WHERE created_at >= :fromInstant
                   AND created_at < :toInstant
                 GROUP BY created_at::date
            )
            SELECT days.metric_date, COALESCE(booking_counts.total_bookings, 0) AS total_bookings
              FROM days
              LEFT JOIN booking_counts ON booking_counts.metric_date = days.metric_date
             ORDER BY days.metric_date
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RevenueMetricResult getRevenue(Instant fromInstant, Instant toInstant, String currency) {
        return jdbcTemplate.queryForObject(
                REVENUE_AGGREGATE,
                Map.of(
                        "fromInstant", hotelTimestamp(fromInstant),
                        "toInstant", hotelTimestamp(toInstant),
                        "currency", currency),
                (rs, rowNum) -> revenueMetric(rs));
    }

    public List<DailyRevenueMetricResult> getDailyRevenue(
            LocalDate fromDate, LocalDate toDate, Instant fromInstant, Instant toInstant, String currency) {
        return jdbcTemplate.query(
                DAILY_REVENUE, dateTimeParameters(fromDate, toDate, fromInstant, toInstant, currency), (rs, rowNum) -> {
                    long grossRevenue = rs.getLong("gross_revenue");
                    long refundedAmount = rs.getLong("refunded_amount");
                    return new DailyRevenueMetricResult(
                            rs.getObject("metric_date", LocalDate.class),
                            grossRevenue,
                            refundedAmount,
                            grossRevenue - refundedAmount);
                });
    }

    public OccupancyMetricResult getOccupancy(LocalDate fromDate, LocalDate toDate) {
        return jdbcTemplate.queryForObject(
                OCCUPANCY_AGGREGATE,
                dateParameters(fromDate, toDate),
                (rs, rowNum) -> new OccupancyMetricResult(
                        rs.getLong("occupied_room_nights"), rs.getLong("available_room_nights")));
    }

    public List<DailyOccupancyMetricResult> getDailyOccupancy(LocalDate fromDate, LocalDate toDate) {
        return jdbcTemplate.query(
                DAILY_OCCUPANCY,
                dateParameters(fromDate, toDate),
                (rs, rowNum) -> new DailyOccupancyMetricResult(
                        rs.getObject("metric_date", LocalDate.class),
                        rs.getLong("occupied_room_nights"),
                        rs.getLong("available_room_nights")));
    }

    public BookingMetricResult getBookingMetrics(Instant fromInstant, Instant toInstant) {
        return jdbcTemplate.queryForObject(
                BOOKING_AGGREGATE,
                Map.of("fromInstant", hotelTimestamp(fromInstant), "toInstant", hotelTimestamp(toInstant)),
                (rs, rowNum) -> new BookingMetricResult(
                        rs.getLong("total_bookings"),
                        rs.getLong("pending_bookings"),
                        rs.getLong("confirmed_bookings"),
                        rs.getLong("checked_in_bookings"),
                        rs.getLong("checked_out_bookings"),
                        rs.getLong("cancelled_bookings"),
                        rs.getLong("expired_bookings"),
                        rs.getLong("total_booking_value")));
    }

    public List<DailyBookingMetricResult> getDailyBookingMetrics(
            LocalDate fromDate, LocalDate toDate, Instant fromInstant, Instant toInstant) {
        return jdbcTemplate.query(
                DAILY_BOOKINGS,
                dateTimeParameters(fromDate, toDate, fromInstant, toInstant, null),
                (rs, rowNum) -> new DailyBookingMetricResult(
                        rs.getObject("metric_date", LocalDate.class), rs.getLong("total_bookings")));
    }

    public java.util.List<com.tpanh.hotel.reporting.dto.response.DashboardReportResponse.DailyMaintenanceExpense>
            getDailyMaintenanceExpense(LocalDate from, LocalDate to) {
        return jdbcTemplate.query(
                """
            WITH days AS (
                SELECT generate_series(CAST(:fromDate AS date), CAST(:toDate AS date)-1, interval '1 day')::date metric_date
            ), costs AS (
                SELECT expense_date, SUM(amount) amount FROM maintenance_costs
                WHERE expense_date>=:fromDate AND expense_date<:toDate GROUP BY expense_date
            ) SELECT days.metric_date, COALESCE(costs.amount,0) amount FROM days
              LEFT JOIN costs ON costs.expense_date=days.metric_date ORDER BY days.metric_date
            """,
                dateParameters(from, to),
                (rs, row) -> new com.tpanh.hotel.reporting.dto.response.DashboardReportResponse.DailyMaintenanceExpense(
                        rs.getObject("metric_date", LocalDate.class), rs.getLong("amount")));
    }

    public long countCompletedMaintenanceWithoutCost(LocalDate from, LocalDate to) {
        return jdbcTemplate.queryForObject(
                """
            SELECT COUNT(*) FROM maintenance_requests r
            WHERE r.status='COMPLETED'
              AND (r.completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date>=:fromDate
              AND (r.completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date<:toDate
              AND NOT EXISTS(SELECT 1 FROM maintenance_costs c WHERE c.request_id=r.id)
            """,
                dateParameters(from, to),
                Long.class);
    }

    private RevenueMetricResult revenueMetric(ResultSet rs) throws SQLException {
        long grossRevenue = rs.getLong("gross_revenue");
        long refundedAmount = rs.getLong("refunded_amount");
        return new RevenueMetricResult(grossRevenue, refundedAmount, grossRevenue - refundedAmount);
    }

    private Map<String, Object> dateParameters(LocalDate fromDate, LocalDate toDate) {
        return Map.of("fromDate", Date.valueOf(fromDate), "toDate", Date.valueOf(toDate));
    }

    private Map<String, Object> dateTimeParameters(
            LocalDate fromDate, LocalDate toDate, Instant fromInstant, Instant toInstant, String currency) {
        var parameters = new java.util.HashMap<String, Object>();
        parameters.putAll(dateParameters(fromDate, toDate));
        parameters.put("fromInstant", hotelTimestamp(fromInstant));
        parameters.put("toInstant", hotelTimestamp(toInstant));
        if (currency != null) {
            parameters.put("currency", currency);
        }
        return parameters;
    }

    private Timestamp hotelTimestamp(Instant instant) {
        return Timestamp.valueOf(LocalDateTime.ofInstant(instant, HOTEL_ZONE));
    }
}
