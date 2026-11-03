package com.tpanh.hotel.room.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tpanh.hotel.room.domain.*;
import com.tpanh.hotel.room.dto.request.RoomWriteRequest;
import com.tpanh.hotel.room.exception.RoomValidationException;
import com.tpanh.hotel.room.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class RoomManagementServiceTest {
    private final RoomRepository rooms = mock(RoomRepository.class);
    private final RoomTypeRepository types = mock(RoomTypeRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final RoomManagementService service = new RoomManagementService(rooms, types, jdbc);

    @Test
    void cannotDeleteInventoryWithOutstandingBookings() {
        UUID id = UUID.randomUUID(), typeId = UUID.randomUUID();
        RoomType type = RoomType.builder().id(typeId).build();
        Room room = Room.builder()
                .id(id)
                .roomType(type)
                .status(RoomStatus.AVAILABLE)
                .build();
        when(rooms.findRoomTypeId(id)).thenReturn(Optional.of(typeId));
        when(types.findByIdForUpdate(typeId)).thenReturn(Optional.of(type));
        when(rooms.findByIdForUpdate(id)).thenReturn(Optional.of(room));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(typeId))).thenReturn(1);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(RoomValidationException.class);
        verify(rooms, never()).delete(any());
    }

    @Test
    void cannotCreateRoomForInactiveType() {
        UUID typeId = UUID.randomUUID();
        when(types.findByIdForUpdate(typeId))
                .thenReturn(Optional.of(
                        RoomType.builder().status(RoomTypeStatus.INACTIVE).build()));
        assertThatThrownBy(() -> service.create(new RoomWriteRequest(typeId, "101", 1, "")))
                .isInstanceOf(RoomValidationException.class);
        verify(rooms, never()).saveAndFlush(any());
    }

    @Test
    void cannotEditOccupiedRoom() {
        Room room = Room.builder().roomNumber("101").status(RoomStatus.OCCUPIED).build();
        assertThatThrownBy(() -> room.updateDetails(RoomType.builder().build(), "102", 2, "changed"))
                .isInstanceOf(RoomValidationException.class);
        assertThat(room.getRoomNumber()).isEqualTo("101");
    }
}
