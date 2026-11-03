package com.tpanh.hotel.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.tpanh.hotel.maintenance.domain.MaintenanceRequestStatus;
import com.tpanh.hotel.maintenance.dto.request.CreateMaintenanceRequestRequest;
import com.tpanh.hotel.maintenance.dto.request.UpdateMaintenanceRequestStatusRequest;
import com.tpanh.hotel.maintenance.repository.MaintenanceRequestRepository;
import com.tpanh.hotel.maintenance.service.MaintenanceRequestService;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.exception.RoomStateConflictException;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.repository.UserRepository;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MaintenanceRequestPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.docker.compose.enabled", () -> false);
    }

    @Autowired
    private MaintenanceRequestService maintenanceRequestService;

    @Autowired
    private MaintenanceRequestRepository maintenanceRequestRepository;

    @Autowired
    private com.tpanh.hotel.room.service.RoomMaintenanceService roomMaintenanceService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private com.tpanh.hotel.maintenance.service.MaintenanceCostService costService;

    private User manager;
    private Room room;

    @BeforeEach
    void setUp() {
        RoomType roomType = roomTypeRepository.saveAndFlush(RoomType.builder()
                .code("MNT_" + suffix())
                .name("Maintenance integration room")
                .basePrice(1_000_000)
                .capacityAdults(2)
                .capacityChildren(1)
                .maxGuests(3)
                .build());
        room = roomRepository.saveAndFlush(Room.builder()
                .roomType(roomType)
                .roomNumber("MNT-" + suffix())
                .floor(1)
                .status(RoomStatus.AVAILABLE)
                .build());
        manager = userRepository.saveAndFlush(User.builder()
                .email("manager-" + suffix() + "@hanbay.test")
                .fullName("Maintenance Manager")
                .build());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new TestingAuthenticationToken(manager.getId().toString(), null, "ROLE_MANAGER"));
    }

    @AfterEach
    void clearSecurityContextAndFailureTrigger() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_maintenance_insert ON maintenance_requests");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS fail_maintenance_insert()");
    }

    @Test
    void recordsCostAndAuditsCorrectionsWithoutDoubleCounting() {
        var request = maintenanceRequestService.create(
                new CreateMaintenanceRequestRequest(room.getId(), "Sửa điều hòa", null));
        var today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        assertThat(costService.get(request.id())).isNull();
        var first = new com.tpanh.hotel.maintenance.dto.request.RecordMaintenanceCostRequest(
                250000L, today, "Tiền sửa điều hòa", 0);
        assertThatThrownBy(() -> costService.record(request.id(), first))
                .isInstanceOf(com.tpanh.hotel.common.exception.RequestValidationException.class);
        maintenanceRequestService.updateStatus(
                request.id(), new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.IN_PROGRESS));
        var saved = costService.record(request.id(), first);
        assertThat(saved.amount()).isEqualTo(250000);
        assertThat(saved.recordedBy()).isEqualTo(manager.getId());
        assertThatThrownBy(() -> costService.record(request.id(), first))
                .isInstanceOf(com.tpanh.hotel.maintenance.service.MaintenanceCostConflictException.class);
        costService.record(
                request.id(),
                new com.tpanh.hotel.maintenance.dto.request.RecordMaintenanceCostRequest(
                        200000L, today, "Nhà cung cấp giảm 50000", 1));
        assertThat(costService.get(request.id()).amount()).isEqualTo(200000);
        assertThat(costService.history(request.id()))
                .extracting(item -> item.amount())
                .containsExactly(200000L, 250000L);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT SUM(amount) FROM maintenance_costs WHERE request_id=?", Long.class, request.id()))
                .isEqualTo(200000L);
        assertThatThrownBy(() -> costService.record(
                        request.id(),
                        new com.tpanh.hotel.maintenance.dto.request.RecordMaintenanceCostRequest(-1L, today, "Sai", 2)))
                .isInstanceOf(com.tpanh.hotel.common.exception.RequestValidationException.class);
        assertThatThrownBy(() -> costService.record(
                        request.id(),
                        new com.tpanh.hotel.maintenance.dto.request.RecordMaintenanceCostRequest(
                                1L, today.plusDays(1), "Tương lai", 2)))
                .isInstanceOf(com.tpanh.hotel.common.exception.RequestValidationException.class);
        assertThat(costService.history(request.id())).hasSize(2);
    }

    @Test
    void persistsRequestAndRoomLifecycleInTheSameUseCases() {
        var created = maintenanceRequestService.create(
                new CreateMaintenanceRequestRequest(room.getId(), "Điều hòa hỏng", "Không làm lạnh"));

        assertThat(created.status()).isEqualTo(MaintenanceRequestStatus.PENDING);
        assertThat(created.roomStatus()).isEqualTo(RoomStatus.MAINTENANCE);
        assertThat(created.createdById()).isEqualTo(manager.getId());
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getStatus())
                .isEqualTo(RoomStatus.MAINTENANCE);

        var inProgress = maintenanceRequestService.updateStatus(
                created.id(), new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.IN_PROGRESS));
        var completed = maintenanceRequestService.updateStatus(
                created.id(), new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.COMPLETED));

        assertThat(inProgress.startedAt()).isNotNull();
        assertThat(completed.status()).isEqualTo(MaintenanceRequestStatus.COMPLETED);
        assertThat(completed.startedAt()).isCloseTo(inProgress.startedAt(), within(1, ChronoUnit.MICROS));
        assertThat(completed.completedAt()).isNotNull();
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getStatus())
                .isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    void rollsBackRoomChangeWhenRequestInsertFails() {
        long requestCountBefore = maintenanceRequestRepository.count();
        jdbcTemplate.execute(
                "CREATE FUNCTION fail_maintenance_insert() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'forced maintenance insert failure'; END; $$");
        jdbcTemplate.execute(
                "CREATE TRIGGER fail_maintenance_insert BEFORE INSERT ON maintenance_requests FOR EACH ROW EXECUTE FUNCTION fail_maintenance_insert()");

        assertThatThrownBy(() -> maintenanceRequestService.create(
                        new CreateMaintenanceRequestRequest(room.getId(), "TV hỏng", null)))
                .isInstanceOf(DataAccessException.class);

        assertThat(maintenanceRequestRepository.count()).isEqualTo(requestCountBefore);
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getStatus())
                .isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    void rollsBackRequestCompletionWhenRoomSynchronizationFails() {
        UUID requestId = maintenanceRequestService
                .create(new CreateMaintenanceRequestRequest(room.getId(), "Máy nước nóng hỏng", null))
                .id();
        maintenanceRequestService.updateStatus(
                requestId, new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.IN_PROGRESS));
        jdbcTemplate.update("UPDATE rooms SET status = CAST('AVAILABLE' AS room_status) WHERE id = ?", room.getId());

        assertThatThrownBy(() -> maintenanceRequestService.updateStatus(
                        requestId, new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.COMPLETED)))
                .isInstanceOf(RoomStateConflictException.class);

        var persisted = maintenanceRequestRepository.findDetailsById(requestId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(MaintenanceRequestStatus.IN_PROGRESS);
        assertThat(persisted.getCompletedAt()).isNull();
    }

    @Test
    void lockedRequestPreventsConcurrentStatusUpdates() throws Exception {
        UUID requestId = maintenanceRequestService
                .create(new CreateMaintenanceRequestRequest(room.getId(), "Khóa cửa lỗi", null))
                .id();
        CountDownLatch firstLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirstLock = new CountDownLatch(1);
        CountDownLatch secondLockAttempted = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);

        try {
            var first = executor.submit(() -> transactions.executeWithoutResult(status -> {
                maintenanceRequestRepository.findByIdForUpdate(requestId).orElseThrow();
                firstLockAcquired.countDown();
                await(releaseFirstLock);
            }));
            assertThat(firstLockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

            var second = executor.submit(() -> transactions.executeWithoutResult(status -> {
                secondLockAttempted.countDown();
                maintenanceRequestRepository.findByIdForUpdate(requestId).orElseThrow();
            }));
            assertThat(secondLockAttempted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);

            releaseFirstLock.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            releaseFirstLock.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void legacyRoomCommandCannotReleaseRoomWithOpenRequest() {
        var request = maintenanceRequestService.create(
                new CreateMaintenanceRequestRequest(room.getId(), "Điều hòa hỏng", null));
        assertThatThrownBy(() -> roomMaintenanceService.changeMaintenanceStatus(
                        room.getRoomNumber(), com.tpanh.hotel.room.domain.RoomMaintenanceAction.END_MAINTENANCE))
                .isInstanceOf(RoomStateConflictException.class)
                .extracting(exception -> ((RoomStateConflictException) exception).getErrorCode())
                .isEqualTo("ROOM_HAS_ACTIVE_MAINTENANCE_REQUEST");
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getStatus())
                .isEqualTo(RoomStatus.MAINTENANCE);
        maintenanceRequestService.updateStatus(
                request.id(), new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.IN_PROGRESS));
        maintenanceRequestService.updateStatus(
                request.id(), new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.COMPLETED));
        var next = maintenanceRequestService.create(
                new CreateMaintenanceRequestRequest(room.getId(), "Kiểm tra khóa cửa", null));
        assertThat(next.status()).isEqualTo(MaintenanceRequestStatus.PENDING);
    }

    @Test
    void databaseRejectsSecondOpenRequestForSameRoom() {
        maintenanceRequestService.create(new CreateMaintenanceRequestRequest(room.getId(), "Điều hòa hỏng", null));
        assertThatThrownBy(() -> jdbcTemplate.update(
                        "INSERT INTO maintenance_requests(room_id,title,created_by) VALUES (?,?,?)",
                        room.getId(),
                        "Yêu cầu trùng",
                        manager.getId()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while holding the database lock");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
