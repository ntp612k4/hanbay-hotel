package com.tpanh.hotel.room.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tpanh.hotel.room.exception.RoomStateConflictException;
import org.junit.jupiter.api.Test;

class RoomHousekeepingTest {

    @Test
    void followsCanonicalHousekeepingStateMachine() {
        Room room = room(RoomStatus.DIRTY);

        room.startCleaning();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLEANING);

        room.completeCleaning();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    void startCleaningRejectsEveryStateExceptDirtyWithoutMutation() {
        for (RoomStatus status : RoomStatus.values()) {
            if (status == RoomStatus.DIRTY) {
                continue;
            }
            Room room = room(status);

            assertThatThrownBy(room::startCleaning)
                    .isInstanceOfSatisfying(
                            RoomStateConflictException.class, exception -> assertThat(exception.getErrorCode())
                                    .isEqualTo("ROOM_CLEANING_START_CONFLICT"));
            assertThat(room.getStatus()).isEqualTo(status);
        }
    }

    @Test
    void completeCleaningRejectsEveryStateExceptCleaningWithoutMutation() {
        for (RoomStatus status : RoomStatus.values()) {
            if (status == RoomStatus.CLEANING) {
                continue;
            }
            Room room = room(status);

            assertThatThrownBy(room::completeCleaning)
                    .isInstanceOfSatisfying(
                            RoomStateConflictException.class, exception -> assertThat(exception.getErrorCode())
                                    .isEqualTo("ROOM_CLEANING_COMPLETE_CONFLICT"));
            assertThat(room.getStatus()).isEqualTo(status);
        }
    }

    private Room room(RoomStatus status) {
        return Room.builder().roomNumber("HK-101").floor(1).status(status).build();
    }
}
