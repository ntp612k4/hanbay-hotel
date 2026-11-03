package com.tpanh.hotel.features.room.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeStatus;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
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
class RoomTypeRepositoryIntegrationTest {

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
    private RoomTypeRepository roomTypeRepository;

    @Test
    void catalogQueryReturnsOnlyActiveCaseInsensitiveNameMatches() {
        String uniqueName = "CatalogTest-" + UUID.randomUUID();
        RoomType activeRoomType = roomType(uniqueName + " Deluxe", RoomTypeStatus.ACTIVE);
        RoomType inactiveRoomType = roomType(uniqueName + " Inactive", RoomTypeStatus.INACTIVE);
        RoomType unrelatedRoomType = roomType("Unrelated-" + UUID.randomUUID(), RoomTypeStatus.ACTIVE);
        roomTypeRepository.saveAllAndFlush(List.of(activeRoomType, inactiveRoomType, unrelatedRoomType));

        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
        Page<RoomType> result = roomTypeRepository.findByStatusAndNameContainingIgnoreCase(
                RoomTypeStatus.ACTIVE, uniqueName.toUpperCase(), pageRequest);

        assertThat(result.getContent()).extracting(RoomType::getName).containsExactly(activeRoomType.getName());
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    private RoomType roomType(String name, RoomTypeStatus status) {
        return RoomType.builder()
                .code("CATALOG_" + UUID.randomUUID().toString().replace("-", ""))
                .name(name)
                .description("Integration test room type")
                .capacityAdults(2)
                .capacityChildren(1)
                .basePrice(1000000L)
                .maxGuests(3)
                .status(status)
                .build();
    }
}
