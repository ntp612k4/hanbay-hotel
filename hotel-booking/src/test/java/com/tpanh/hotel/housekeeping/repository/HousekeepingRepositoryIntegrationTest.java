package com.tpanh.hotel.housekeeping.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class HousekeepingRepositoryIntegrationTest {

    private static final Sort DIRTY_ROOM_SORT = Sort.by(Sort.Order.asc("floor"), Sort.Order.asc("roomNumber"));

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
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void queriesOnlyDirtyRoomsWithStableSortAndFetchedRoomType() {
        RoomType roomType = roomTypeRepository.saveAndFlush(roomType());
        Room floorTwo = room("HK-201-" + suffix(), 2, RoomStatus.DIRTY, roomType);
        Room floorOneSecond = room("HK-102-" + suffix(), 1, RoomStatus.DIRTY, roomType);
        Room floorOneFirst = room("HK-101-" + suffix(), 1, RoomStatus.DIRTY, roomType);
        Room available = room("HK-103-" + suffix(), 1, RoomStatus.AVAILABLE, roomType);
        roomRepository.saveAllAndFlush(List.of(floorTwo, floorOneSecond, floorOneFirst, available));
        entityManager.clear();

        var firstPage = roomRepository.findAllByStatus(RoomStatus.DIRTY, PageRequest.of(0, 2, DIRTY_ROOM_SORT));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent())
                .extracting(Room::getRoomNumber)
                .containsExactly(floorOneFirst.getRoomNumber(), floorOneSecond.getRoomNumber());
        assertThat(firstPage.getContent()).allSatisfy(room -> {
            assertThat(room.getStatus()).isEqualTo(RoomStatus.DIRTY);
            assertThat(Hibernate.isInitialized(room.getRoomType())).isTrue();
        });
    }

    private RoomType roomType() {
        return RoomType.builder()
                .code("HK_" + suffix())
                .name("Housekeeping Test Deluxe")
                .description("Integration test room type")
                .capacityAdults(2)
                .capacityChildren(1)
                .basePrice(1_000_000L)
                .maxGuests(3)
                .build();
    }

    private Room room(String roomNumber, int floor, RoomStatus status, RoomType roomType) {
        return Room.builder()
                .roomType(roomType)
                .roomNumber(roomNumber)
                .floor(floor)
                .status(status)
                .build();
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
