package com.tpanh.hotel.housekeeping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class HousekeepingApiIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Test
    @WithMockUser(roles = "HOUSEKEEPER")
    void returnsOnlyDirtyRoomsThroughControllerServiceAndRepository() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        RoomType roomType = roomTypeRepository.saveAndFlush(RoomType.builder()
                .code("HK_API_" + suffix)
                .name("Housekeeping API Test")
                .description("Integration test room type")
                .capacityAdults(2)
                .capacityChildren(1)
                .basePrice(1_000_000L)
                .maxGuests(3)
                .build());
        Room floorTwo = room("HK-201-" + suffix, 2, RoomStatus.DIRTY, "Ưu tiên thay ga giường", roomType);
        Room floorOne = room("HK-101-" + suffix, 1, RoomStatus.DIRTY, null, roomType);
        Room available = room("HK-102-" + suffix, 1, RoomStatus.AVAILABLE, null, roomType);
        roomRepository.saveAllAndFlush(List.of(floorTwo, floorOne, available));

        mockMvc.perform(get("/api/v1/housekeeping/dirty-rooms")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].roomNumber").value(floorOne.getRoomNumber()))
                .andExpect(jsonPath("$.data.content[0].floor").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("DIRTY"))
                .andExpect(jsonPath("$.data.content[0].note").isEmpty())
                .andExpect(jsonPath("$.data.content[0].roomType.code").value(roomType.getCode()))
                .andExpect(jsonPath("$.data.content[1].roomNumber").value(floorTwo.getRoomNumber()))
                .andExpect(jsonPath("$.data.content[1].note").value("Ưu tiên thay ga giường"));
    }

    @Test
    @WithMockUser(roles = "HOUSEKEEPER")
    void completesCleaningAndRemovesRoomFromRoomsRequiringCleaning() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        RoomType roomType = roomTypeRepository.saveAndFlush(RoomType.builder()
                .code("HK_COMPLETE_" + suffix)
                .name("Housekeeping Complete Cleaning Test")
                .description("Integration test room type")
                .capacityAdults(2)
                .capacityChildren(1)
                .basePrice(1_000_000L)
                .maxGuests(3)
                .build());
        Room cleaningRoom = room("HK-CLEAN-" + suffix, 1, RoomStatus.CLEANING, null, roomType);
        roomRepository.saveAndFlush(cleaningRoom);

        mockMvc.perform(patch("/api/v1/housekeeping/rooms/{roomId}/complete-cleaning", cleaningRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã hoàn tất dọn phòng"))
                .andExpect(jsonPath("$.data.roomId").value(cleaningRoom.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));

        assertThat(roomRepository.findById(cleaningRoom.getId()).orElseThrow().getStatus())
                .isEqualTo(RoomStatus.AVAILABLE);
        mockMvc.perform(get("/api/v1/housekeeping/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.roomId == '%s')]".formatted(cleaningRoom.getId()))
                        .isEmpty());
    }

    private Room room(String roomNumber, int floor, RoomStatus status, String note, RoomType roomType) {
        return Room.builder()
                .roomType(roomType)
                .roomNumber(roomNumber)
                .floor(floor)
                .status(status)
                .note(note)
                .build();
    }
}
