package com.tpanh.hotel.room.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomMaintenanceAction;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.exception.RoomNotFoundException;
import com.tpanh.hotel.room.exception.RoomStateConflictException;
import com.tpanh.hotel.room.repository.RoomRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomMaintenanceServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private com.tpanh.hotel.maintenance.repository.MaintenanceRequestRepository maintenanceRequestRepository;

    @InjectMocks
    private RoomMaintenanceServiceImpl service;

    @Test
    void loadsRoomWithWriteLockAndStartsMaintenance() {
        Room room = Room.builder()
                .id(UUID.randomUUID())
                .roomNumber("0501")
                .status(RoomStatus.AVAILABLE)
                .build();
        when(roomRepository.findByRoomNumberForUpdate("0501")).thenReturn(Optional.of(room));
        var response = service.changeMaintenanceStatus(" 0501 ", RoomMaintenanceAction.START_MAINTENANCE);
        assertThat(response.status()).isEqualTo(RoomStatus.MAINTENANCE);
    }

    @Test
    void rejectsUnknownActionAndMissingRoom() {
        assertThatThrownBy(() -> service.changeMaintenanceStatus("0501", RoomMaintenanceAction.UNKNOWN))
                .isInstanceOf(RequestValidationException.class);
        when(roomRepository.findByRoomNumberForUpdate("9999")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.changeMaintenanceStatus("9999", RoomMaintenanceAction.END_MAINTENANCE))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void safelyRejectsDuplicateTransitionAfterLockedRead() {
        Room room =
                Room.builder().roomNumber("0501").status(RoomStatus.MAINTENANCE).build();
        when(roomRepository.findByRoomNumberForUpdate("0501")).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> service.changeMaintenanceStatus("0501", RoomMaintenanceAction.START_MAINTENANCE))
                .isInstanceOf(RoomStateConflictException.class);
    }
}
