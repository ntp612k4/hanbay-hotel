package com.tpanh.hotel.seed.config;

import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeStatus;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@RequiredArgsConstructor
public class RoomDataInitializer implements ApplicationRunner {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roomRepository.count() == 0) {
            seedRoomsForAllActiveTypes();
        }

        seedOceanSuiteRooms();
    }

    private void seedRoomsForAllActiveTypes() {
        List<RoomType> types = roomTypeRepository.findAllByStatusOrderByNameAsc(RoomTypeStatus.ACTIVE);
        if (types.isEmpty()) return;

        List<Room> rooms = new ArrayList<>();
        AtomicInteger globalCounter = new AtomicInteger(100);

        for (RoomType type : types) {
            for (int i = 0; i < 5; i++) {
                Room r = Room.builder()
                        .roomType(type)
                        .roomNumber(type.getCode() + "-" + (globalCounter.incrementAndGet()))
                        .floor(1 + (i % 3))
                        .status(RoomStatus.AVAILABLE)
                        .note(null)
                        .build();
                rooms.add(r);
            }
        }

        roomRepository.saveAll(rooms);
    }

    private void seedOceanSuiteRooms() {
        jdbcTemplate.update(
                """
                INSERT INTO rooms (id, room_type_id, room_number, floor, status, note, created_at, updated_at)
                SELECT inventory.id,
                       room_type.id,
                       inventory.room_number,
                       inventory.floor,
                       'AVAILABLE'::room_status,
                       'Dữ liệu mẫu cho Phòng Suite Hướng Biển Cao Cấp',
                       CURRENT_TIMESTAMP,
                       CURRENT_TIMESTAMP
                FROM (
                    VALUES
                        ('b1000000-0000-4000-8000-000000000010'::UUID, 'OS701', 7),
                        ('b1000000-0000-4000-8000-000000000011'::UUID, 'OS702', 7),
                        ('b1000000-0000-4000-8000-000000000012'::UUID, 'OS801', 8)
                ) AS inventory(id, room_number, floor)
                JOIN room_types room_type ON room_type.code = 'HANBAY-OCEAN-SUITE'
                WHERE NOT EXISTS (
                    SELECT 1 FROM rooms existing WHERE existing.room_type_id = room_type.id
                )
                ON CONFLICT (room_number) DO NOTHING
                """);
    }
}
