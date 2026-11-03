package com.tpanh.hotel.room.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tpanh.hotel.room.exception.RoomStateConflictException;
import org.junit.jupiter.api.Test;

class RoomMaintenanceTest {

    @Test
    void startsAndEndsMaintenanceFromValidStates() {
        Room room = room(RoomStatus.AVAILABLE);
        room.startMaintenance();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
        room.endMaintenance();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    void rejectsStartingMaintenanceFromEveryNonAvailableState() {
        for (RoomStatus status :
                new RoomStatus[] {RoomStatus.OCCUPIED, RoomStatus.DIRTY, RoomStatus.CLEANING, RoomStatus.MAINTENANCE}) {
            assertThatThrownBy(() -> room(status).startMaintenance()).isInstanceOf(RoomStateConflictException.class);
        }
    }

    @Test
    void rejectsEndingMaintenanceWhenNotUnderMaintenance() {
        assertThatThrownBy(() -> room(RoomStatus.AVAILABLE).endMaintenance())
                .isInstanceOf(RoomStateConflictException.class);
    }

    private Room room(RoomStatus status) {
        return Room.builder().roomNumber("0501").floor(5).status(status).build();
    }
}
