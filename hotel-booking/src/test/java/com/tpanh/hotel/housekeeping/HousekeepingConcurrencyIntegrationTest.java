package com.tpanh.hotel.housekeeping;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpanh.hotel.housekeeping.service.HousekeepingService;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.exception.RoomStateConflictException;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class HousekeepingConcurrencyIntegrationTest {

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
    private HousekeepingService housekeepingService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void serializesConcurrentCleaningStartsAndAllowsExactlyOneTransition() throws Exception {
        UUID roomId = createDirtyRoom();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> attempt = () -> {
            ready.countDown();
            start.await();
            try {
                housekeepingService.startCleaning(roomId);
                return "SUCCESS";
            } catch (RoomStateConflictException exception) {
                return exception.getErrorCode();
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> outcomes = List.of(executor.submit(attempt), executor.submit(attempt));
            ready.await();
            start.countDown();

            assertThat(outcomes.stream().map(this::get))
                    .containsExactlyInAnyOrder("SUCCESS", "ROOM_CLEANING_START_CONFLICT");
        } finally {
            executor.shutdownNow();
        }

        assertThat(roomRepository.findById(roomId).orElseThrow().getStatus()).isEqualTo(RoomStatus.CLEANING);
    }

    @Test
    void serializesConcurrentCleaningCompletionsAndAllowsExactlyOneTransition() throws Exception {
        UUID roomId = createRoom(RoomStatus.CLEANING);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> attempt = () -> {
            ready.countDown();
            start.await();
            try {
                housekeepingService.completeCleaning(roomId);
                return "SUCCESS";
            } catch (RoomStateConflictException exception) {
                return exception.getErrorCode();
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> outcomes = List.of(executor.submit(attempt), executor.submit(attempt));
            ready.await();
            start.countDown();

            assertThat(outcomes.stream().map(this::get))
                    .containsExactlyInAnyOrder("SUCCESS", "ROOM_CLEANING_COMPLETE_CONFLICT");
        } finally {
            executor.shutdownNow();
        }

        assertThat(roomRepository.findById(roomId).orElseThrow().getStatus()).isEqualTo(RoomStatus.AVAILABLE);
    }

    private UUID createDirtyRoom() {
        return createRoom(RoomStatus.DIRTY);
    }

    private UUID createRoom(RoomStatus initialStatus) {
        return transactionTemplate.execute(transactionStatus -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            RoomType roomType = roomTypeRepository.save(RoomType.builder()
                    .code("HK_CON_" + suffix)
                    .name("Housekeeping Concurrency Test")
                    .description("Integration test room type")
                    .capacityAdults(2)
                    .capacityChildren(1)
                    .basePrice(1_000_000L)
                    .maxGuests(3)
                    .build());
            return roomRepository
                    .save(Room.builder()
                            .roomType(roomType)
                            .roomNumber("HK-CON-" + suffix)
                            .floor(1)
                            .status(initialStatus)
                            .build())
                    .getId();
        });
    }

    private String get(Future<String> outcome) {
        try {
            return outcome.get();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
