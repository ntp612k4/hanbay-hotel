package com.tpanh.hotel.room.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.dto.response.PhysicalRoomListItemResponse;
import com.tpanh.hotel.room.repository.RoomRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    @Test
    void returnsPhysicalRoomsWithTheirCurrentStatuses() {
        Room room = Room.builder()
                .id(UUID.fromString("10000000-0000-0000-0000-000000000001"))
                .roomNumber("DLX-101")
                .floor(1)
                .status(RoomStatus.MAINTENANCE)
                .build();
        when(roomRepository.findAllByOrderByFloorAscRoomNumberAsc(any())).thenReturn(new PageImpl<>(List.of(room)));

        PageResponse<PhysicalRoomListItemResponse> response = roomService.findAll(0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).toString()).contains("DLX-101").contains("MAINTENANCE");
        verify(roomRepository).findAllByOrderByFloorAscRoomNumberAsc(any());
    }

    @Test
    void rejectsInvalidPagination() {
        assertThatThrownBy(() -> roomService.findAll(-1, 20)).isInstanceOf(RequestValidationException.class);
    }
}
