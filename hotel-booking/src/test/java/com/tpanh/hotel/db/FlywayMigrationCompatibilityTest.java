package com.tpanh.hotel.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationCompatibilityTest {

    private static final String DEVELOP_VERSION = "202609051400";
    private static final String PRE_HOLIDAY_VERSION = "202609041200";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg17")
            .withDatabaseName("flyway_compatibility_test")
            .withUsername("test")
            .withPassword("test");

    @BeforeEach
    void cleanDatabase() {
        flyway().clean();
    }

    @Test
    void migratesFreshDatabase() throws Exception {
        flyway().migrate();

        assertIdentitySchemaExists();
    }

    @Test
    void migratesDatabaseAlreadyAtDevelopVersion() throws Exception {
        flyway(DEVELOP_VERSION).migrate();

        assertThat(tableExists("accounts")).isFalse();
        assertThat(tableExists("users")).isTrue();
        assertThat(tableExists("maintenance_requests")).isFalse();

        flyway().migrate();

        assertIdentitySchemaExists();
    }

    @Test
    void upgradesHolidayRuleWithMaximumLengthVietnameseName() throws Exception {
        flyway(PRE_HOLIDAY_VERSION).migrate();
        String name = "ễ".repeat(150);
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.prepareStatement(
                        """
                    INSERT INTO price_rules (reference_id, room_type_id, name, rule_type,
                        adjustment_type, adjustment_value, start_date, end_date, created_at)
                    VALUES (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', ?,
                        'HOLIDAY', 'FIXED', 100000, '2027-01-01', '2027-01-02', CURRENT_TIMESTAMP)
                    """)) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
        flyway().migrate();
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement();
                var result =
                        statement.executeQuery("SELECT name FROM holiday_calendar WHERE start_date = '2027-01-01'")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isEqualTo(("Lịch lễ chuyển tiếp - " + name).substring(0, 150));
        }
    }

    private void assertIdentitySchemaExists() throws Exception {
        assertThat(tableExists("users")).isTrue();
        assertThat(tableExists("roles")).isTrue();
        assertThat(tableExists("user_roles")).isTrue();
        assertThat(tableExists("refresh_tokens")).isTrue();
        assertThat(tableExists("accounts")).isFalse();
        assertThat(countRows("room_types")).isGreaterThanOrEqualTo(4);
        assertThat(countRows("rooms")).isGreaterThanOrEqualTo(9);
        assertRoomTypeDetailData("10000000-0000-0000-0000-000000000001", 3, 6);
        assertRoomTypeDetailData("a1000000-0000-4000-8000-000000000001", 3, 4);
        assertRoomTypeDetailData("a1000000-0000-4000-8000-000000000002", 3, 5);
        assertRoomTypeDetailData("a1000000-0000-4000-8000-000000000003", 3, 6);
        assertDetailedRoomDescriptions();
        assertThat(tableExists("payments")).isTrue();
        assertThat(tableExists("payment_transactions")).isTrue();
        assertThat(tableExists("payment_webhook_events")).isTrue();
        assertThat(tableExists("payment_refunds")).isTrue();
        assertThat(tableExists("holiday_calendar")).isTrue();
        assertThat(columnExists("price_rules", "adjustment_direction")).isTrue();
        assertThat(columnExists("payments", "currency")).isTrue();
        assertThat(columnExists("payments", "expires_at")).isTrue();
        assertThat(indexExists("idx_payments_status_created_at")).isTrue();
        assertThat(indexExists("idx_payments_status_expires_at")).isTrue();
        assertThat(indexExists("idx_payment_transactions_payment_received_at")).isTrue();
        assertThat(indexExists("idx_payment_webhook_events_status_received_at")).isTrue();
        assertThat(indexExists("idx_payment_refunds_status_requested_at")).isTrue();
        assertMaintenanceRequestSchemaExists();
    }

    private void assertMaintenanceRequestSchemaExists() throws Exception {
        assertThat(tableExists("maintenance_requests")).isTrue();
        assertThat(columnExists("maintenance_requests", "room_id")).isTrue();
        assertThat(columnExists("maintenance_requests", "created_by")).isTrue();
        assertThat(columnExists("maintenance_requests", "status")).isTrue();
        assertThat(columnExists("maintenance_requests", "started_at")).isTrue();
        assertThat(columnExists("maintenance_requests", "completed_at")).isTrue();
        assertThat(indexExists("idx_maintenance_requests_created_at")).isTrue();
        assertThat(indexExists("uq_maintenance_requests_active_room")).isTrue();
    }

    private void assertRoomTypeDetailData(String roomTypeId, long expectedImages, long expectedAmenities)
            throws Exception {
        assertThat(countRowsByRoomType("room_type_images", roomTypeId)).isEqualTo(expectedImages);
        assertThat(countRowsByRoomType("room_type_amenities", roomTypeId)).isEqualTo(expectedAmenities);
    }

    private void assertDetailedRoomDescriptions() throws Exception {
        var descriptions = List.of(
                findRoomDescription("10000000-0000-0000-0000-000000000001"),
                findRoomDescription("a1000000-0000-4000-8000-000000000001"),
                findRoomDescription("a1000000-0000-4000-8000-000000000002"),
                findRoomDescription("a1000000-0000-4000-8000-000000000003"));
        var shortDescriptions = List.of(
                findRoomShortDescription("10000000-0000-0000-0000-000000000001"),
                findRoomShortDescription("a1000000-0000-4000-8000-000000000001"),
                findRoomShortDescription("a1000000-0000-4000-8000-000000000002"),
                findRoomShortDescription("a1000000-0000-4000-8000-000000000003"));

        assertThat(descriptions).doesNotHaveDuplicates();
        assertThat(descriptions).allSatisfy(description -> {
            assertThat(description).hasSizeGreaterThan(300);
            assertThat(description).contains("\n\n");
        });
        assertThat(shortDescriptions).doesNotHaveDuplicates();
        assertThat(shortDescriptions)
                .allSatisfy(shortDescription -> assertThat(shortDescription).hasSizeBetween(80, 240));
    }

    private boolean indexExists(String indexName) throws Exception {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
            statement.setString(1, "public." + indexName);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private boolean columnExists(String tableName, String columnName) throws Exception {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.prepareStatement("SELECT EXISTS (SELECT 1 FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?)")) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private long countRows(String tableName) throws Exception {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            result.next();
            return result.getLong(1);
        }
    }

    private long countRowsByRoomType(String tableName, String roomTypeId) throws Exception {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement =
                        connection.prepareStatement("SELECT COUNT(*) FROM " + tableName + " WHERE room_type_id = ?")) {
            statement.setObject(1, java.util.UUID.fromString(roomTypeId));
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private String findRoomDescription(String roomTypeId) throws Exception {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.prepareStatement("SELECT description FROM room_types WHERE id = ?")) {
            statement.setObject(1, java.util.UUID.fromString(roomTypeId));
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getString(1);
            }
        }
    }

    private String findRoomShortDescription(String roomTypeId) throws Exception {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.prepareStatement("SELECT short_description FROM room_types WHERE id = ?")) {
            statement.setObject(1, java.util.UUID.fromString(roomTypeId));
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getString(1);
            }
        }
    }

    private boolean tableExists(String tableName) throws Exception {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
            statement.setString(1, "public." + tableName);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private Flyway flyway(String... target) {
        var configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .cleanDisabled(false);
        if (target.length == 1) {
            configuration.target(target[0]);
        }
        return configuration.load();
    }
}
