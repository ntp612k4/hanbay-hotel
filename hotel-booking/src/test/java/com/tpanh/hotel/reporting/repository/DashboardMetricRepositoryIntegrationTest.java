package com.tpanh.hotel.reporting.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpanh.hotel.reporting.dto.request.DashboardReportRequest;
import com.tpanh.hotel.reporting.service.DashboardReportService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class DashboardMetricRepositoryIntegrationTest {

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate FROM_DATE = LocalDate.of(2098, 3, 10);
    private static final LocalDate TO_DATE = FROM_DATE.plusDays(3);
    private static final Instant FROM_INSTANT =
            FROM_DATE.atStartOfDay(HOTEL_ZONE).toInstant();
    private static final Instant TO_INSTANT = TO_DATE.atStartOfDay(HOTEL_ZONE).toInstant();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.docker.compose.enabled", () -> false);
    }

    @Autowired
    private DashboardMetricRepository repository;

    @Autowired
    private DashboardReportService reportService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID roomTypeId;
    private long initialRoomCount;

    @BeforeEach
    void setUpInventory() {
        roomTypeId = UUID.randomUUID();
        initialRoomCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rooms", Long.class);
        jdbcTemplate.update(
                """
                INSERT INTO room_types
                    (id, code, name, base_price, max_guests, capacity_adults, capacity_children, status, created_at, updated_at)
                VALUES (?, ?, 'Dashboard Metric Room', 1000000, 2, 2, 0, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                roomTypeId,
                "METRIC_" + UUID.randomUUID().toString().replace("-", ""));
        insertRoom("METRIC-101");
        insertRoom("METRIC-102");
    }

    @Test
    void calculatesNetRevenueFromSuccessfulLedgerTransactionsWithinHalfOpenPeriod() {
        UUID payment = insertPayment(insertBooking("REV", "CONFIRMED", FROM_DATE, TO_DATE, FROM_INSTANT, 900_000));
        insertTransaction(payment, "PAYMENT", "SUCCESS", 1_000_000, FROM_INSTANT.plusSeconds(60));
        insertTransaction(payment, "REFUND", "SUCCESS", 200_000, FROM_INSTANT.plusSeconds(120));
        insertTransaction(payment, "PAYMENT", "FAILED", 500_000, FROM_INSTANT.plusSeconds(180));
        insertTransaction(payment, "PAYMENT", "SUCCESS", 700_000, TO_INSTANT);

        var result = repository.getRevenue(FROM_INSTANT, TO_INSTANT, "VND");
        var daily = repository.getDailyRevenue(FROM_DATE, TO_DATE, FROM_INSTANT, TO_INSTANT, "VND");

        assertThat(result.grossRevenue()).isEqualTo(1_000_000);
        assertThat(result.refundedAmount()).isEqualTo(200_000);
        assertThat(result.netRevenue()).isEqualTo(800_000);
        assertThat(daily).hasSize(3);
        assertThat(daily).extracting(item -> item.netRevenue()).containsExactly(800_000L, 0L, 0L);
    }

    @Test
    void countsOnlyEligibleBookingNightsAndReturnsZeroFilledDailySeries() {
        insertBookingWithNights("CONFIRMED", FROM_DATE.minusDays(1), TO_DATE.plusDays(1));
        insertBookingWithNights("CHECKED_IN", FROM_DATE.plusDays(1), TO_DATE);
        insertBookingWithNights("CHECKED_OUT", FROM_DATE.plusDays(2), TO_DATE);
        insertBookingWithNights("PENDING", FROM_DATE, TO_DATE);
        insertBookingWithNights("CANCELLED", FROM_DATE, TO_DATE);
        insertBookingWithNights("EXPIRED", FROM_DATE, TO_DATE);

        var result = repository.getOccupancy(FROM_DATE, TO_DATE);
        var daily = repository.getDailyOccupancy(FROM_DATE, TO_DATE);

        assertThat(result.occupiedRoomNights()).isEqualTo(6);
        assertThat(result.availableRoomNights()).isEqualTo((initialRoomCount + 2) * 3);
        assertThat(daily).hasSize(3);
        assertThat(daily).extracting(item -> item.occupiedRoomNights()).containsExactly(1L, 2L, 3L);
        assertThat(daily)
                .allSatisfy(item -> assertThat(item.availableRoomNights()).isEqualTo(initialRoomCount + 2));
    }

    @Test
    void groupsBookingsByCurrentStatusAndCreatedDate() {
        insertBooking("PENDING", "PENDING", FROM_DATE, TO_DATE, FROM_INSTANT.plusSeconds(1), 100_000);
        insertBooking("CONFIRMED", "CONFIRMED", FROM_DATE, TO_DATE, FROM_INSTANT.plusSeconds(2), 200_000);
        insertBooking("CANCELLED", "CANCELLED", FROM_DATE, TO_DATE, FROM_INSTANT.plusSeconds(3), 300_000);
        insertBooking("EXPIRED", "EXPIRED", FROM_DATE, TO_DATE, FROM_INSTANT.plusSeconds(4), 400_000);
        insertBooking("BOUNDARY", "CHECKED_OUT", FROM_DATE, TO_DATE, TO_INSTANT, 9_000_000);

        var result = repository.getBookingMetrics(FROM_INSTANT, TO_INSTANT);
        var daily = repository.getDailyBookingMetrics(FROM_DATE, TO_DATE, FROM_INSTANT, TO_INSTANT);

        assertThat(result.totalBookings()).isEqualTo(4);
        assertThat(result.pendingBookings()).isEqualTo(1);
        assertThat(result.confirmedBookings()).isEqualTo(1);
        assertThat(result.cancelledBookings()).isEqualTo(1);
        assertThat(result.expiredBookings()).isEqualTo(1);
        assertThat(result.checkedInBookings()).isZero();
        assertThat(result.checkedOutBookings()).isZero();
        assertThat(result.totalBookingValue()).isEqualTo(1_000_000);
        assertThat(daily).extracting(item -> item.totalBookings()).containsExactly(4L, 0L, 0L);
    }

    @Test
    void dashboardResponseMatchesKnownDatabaseFacts() {
        UUID confirmedBooking =
                insertBooking("QA-CONFIRMED", "CONFIRMED", FROM_DATE, TO_DATE, FROM_INSTANT.plusSeconds(60), 300_000);
        insertBookingNights(confirmedBooking, FROM_DATE, TO_DATE);

        insertBooking("QA-CANCELLED", "CANCELLED", FROM_DATE, TO_DATE, FROM_INSTANT.plusSeconds(86_400 + 60), 200_000);

        UUID checkedOutBooking = insertBooking(
                "QA-CHECKED-OUT",
                "CHECKED_OUT",
                FROM_DATE.plusDays(1),
                TO_DATE,
                FROM_INSTANT.plusSeconds(2 * 86_400 + 60),
                500_000);
        insertBookingNights(checkedOutBooking, FROM_DATE.plusDays(1), TO_DATE);

        UUID payment = insertPayment(confirmedBooking);
        insertTransaction(payment, "PAYMENT", "SUCCESS", 1_200_000, FROM_INSTANT.plusSeconds(120));
        insertTransaction(payment, "REFUND", "SUCCESS", 200_000, FROM_INSTANT.plusSeconds(86_400 + 120));

        var response = reportService.getDashboardReport(new DashboardReportRequest(FROM_DATE, TO_DATE, "VND"));
        long availableRoomNights = (initialRoomCount + 2) * 3;
        BigDecimal expectedOccupancyRate =
                BigDecimal.valueOf(500).divide(BigDecimal.valueOf(availableRoomNights), 2, RoundingMode.HALF_UP);

        assertThat(response.period().from()).isEqualTo(FROM_DATE);
        assertThat(response.period().to()).isEqualTo(TO_DATE);
        assertThat(response.revenue().grossRevenue()).isEqualTo(1_200_000);
        assertThat(response.revenue().refundedAmount()).isEqualTo(200_000);
        assertThat(response.revenue().netRevenue()).isEqualTo(1_000_000);

        assertThat(response.occupancy().occupiedRoomNights()).isEqualTo(5);
        assertThat(response.occupancy().availableRoomNights()).isEqualTo(availableRoomNights);
        assertThat(response.occupancy().occupancyRate()).isEqualByComparingTo(expectedOccupancyRate);

        assertThat(response.bookings().totalBookings()).isEqualTo(3);
        assertThat(response.bookings().confirmedBookings()).isEqualTo(1);
        assertThat(response.bookings().checkedOutBookings()).isEqualTo(1);
        assertThat(response.bookings().cancelledBookings()).isEqualTo(1);
        assertThat(response.bookings().totalBookingValue()).isEqualTo(1_000_000);
        assertThat(response.bookings().averageBookingValue()).isEqualTo(333_333);
        assertThat(response.bookings().cancellationRate()).isEqualByComparingTo("33.33");

        assertThat(response.revenueTrend())
                .extracting(item -> item.netRevenue())
                .containsExactly(1_200_000L, -200_000L, 0L);
        assertThat(response.occupancyTrend())
                .extracting(item -> item.occupiedRoomNights())
                .containsExactly(1L, 2L, 2L);
        assertThat(response.bookingTrend())
                .extracting(item -> item.totalBookings())
                .containsExactly(1L, 1L, 1L);
    }

    @Test
    void separatesCashReceiptsFromMaintenanceExpensesAndMissingCostData() {
        UUID payment = insertPayment(insertBooking("COSTS", "CONFIRMED", FROM_DATE, TO_DATE, FROM_INSTANT, 900000));
        insertTransaction(payment, "PAYMENT", "SUCCESS", 1000000, FROM_INSTANT.plusSeconds(30));
        insertTransaction(payment, "REFUND", "SUCCESS", 200000, FROM_INSTANT.plusSeconds(60));
        UUID user = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users(id,full_name,status) VALUES (?,'Cost tester','ACTIVE')", user);
        UUID room = jdbcTemplate.queryForObject(
                "SELECT id FROM rooms WHERE room_type_id=? LIMIT 1", UUID.class, roomTypeId);
        for (int i = 0; i < 3; i++) {
            UUID request = UUID.randomUUID();
            jdbcTemplate.update(
                    "INSERT INTO maintenance_requests(id,room_id,title,status,created_by,started_at,completed_at) VALUES (?,?,'Sửa phòng','COMPLETED',?,?,?)",
                    request,
                    room,
                    user,
                    FROM_INSTANT.atOffset(java.time.ZoneOffset.UTC),
                    FROM_INSTANT.plusSeconds(120).atOffset(java.time.ZoneOffset.UTC));
            if (i < 2)
                jdbcTemplate.update(
                        "INSERT INTO maintenance_costs(request_id,amount,expense_date,note,revision,recorded_by,recorded_at) VALUES (?,?,?,'Chi phí',1,?,CURRENT_TIMESTAMP)",
                        request,
                        i == 0 ? 300000 : 900000,
                        Date.valueOf(i == 0 ? FROM_DATE : TO_DATE),
                        user);
        }
        var report = reportService.getDashboardReport(new DashboardReportRequest(FROM_DATE, TO_DATE, "VND"));
        assertThat(report.revenue().netRevenue()).isEqualTo(800000);
        assertThat(report.maintenance().recordedExpense()).isEqualTo(300000);
        assertThat(report.maintenance().cashAfterMaintenance()).isEqualTo(500000);
        assertThat(report.maintenance().completedWithoutCost()).isEqualTo(1);
        assertThat(report.maintenance().trend())
                .extracting(item -> item.amount())
                .containsExactly(300000L, 0L, 0L);
        var usd = reportService.getDashboardReport(new DashboardReportRequest(FROM_DATE, TO_DATE, "USD"));
        assertThat(usd.maintenance().recordedExpense()).isZero();
    }

    private void insertRoom(String roomNumber) {
        jdbcTemplate.update(
                """
                INSERT INTO rooms (id, room_type_id, room_number, floor, status, created_at, updated_at)
                VALUES (?, ?, ?, 1, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(),
                roomTypeId,
                roomNumber + "-" + UUID.randomUUID().toString().substring(0, 8));
    }

    private void insertBookingWithNights(String status, LocalDate checkIn, LocalDate checkOut) {
        UUID bookingId = insertBooking(status, status, checkIn, checkOut, FROM_INSTANT, 1_000_000);
        insertBookingNights(bookingId, checkIn, checkOut);
    }

    private void insertBookingNights(UUID bookingId, LocalDate checkIn, LocalDate checkOut) {
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            jdbcTemplate.update(
                    """
                    INSERT INTO booking_nights
                        (id, reference_id, booking_id, stay_date, price_per_night, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 500000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    bookingId,
                    Date.valueOf(date));
        }
    }

    private UUID insertBooking(
            String codeSuffix,
            String status,
            LocalDate checkIn,
            LocalDate checkOut,
            Instant createdAt,
            long totalAmount) {
        UUID bookingId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO bookings
                    (id, booking_code, room_type_id, check_in_date, check_out_date, adult_count, child_count,
                     status, total_amount, paid_amount, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 1, 0, CAST(? AS booking_status), ?, 0, ?, ?)
                """,
                bookingId,
                "METRIC-" + codeSuffix + "-" + UUID.randomUUID().toString().substring(0, 8),
                roomTypeId,
                Date.valueOf(checkIn),
                Date.valueOf(checkOut),
                status,
                totalAmount,
                Timestamp.from(createdAt),
                Timestamp.from(createdAt));
        return bookingId;
    }

    private UUID insertPayment(UUID bookingId) {
        UUID paymentId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO payments
                    (id, booking_id, payment_code, amount, currency, status, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, 1000000, 'VND', 'SUCCESS', ?, ?, ?)
                """,
                paymentId,
                bookingId,
                "PAY-METRIC-" + UUID.randomUUID(),
                Timestamp.from(TO_INSTANT),
                Timestamp.from(FROM_INSTANT),
                Timestamp.from(FROM_INSTANT));
        return paymentId;
    }

    private void insertTransaction(UUID paymentId, String type, String status, long amount, Instant occurredAt) {
        jdbcTemplate.update(
                """
                INSERT INTO payment_transactions
                    (id, payment_id, provider_transaction_id, type, status, amount, currency,
                     occurred_at, received_at, created_at)
                VALUES (?, ?, ?, CAST(? AS payment_transaction_type), CAST(? AS payment_transaction_status),
                        ?, 'VND', ?, ?, ?)
                """,
                UUID.randomUUID(),
                paymentId,
                "TX-METRIC-" + UUID.randomUUID(),
                type,
                status,
                amount,
                Timestamp.from(occurredAt),
                Timestamp.from(occurredAt),
                Timestamp.from(occurredAt));
    }
}
