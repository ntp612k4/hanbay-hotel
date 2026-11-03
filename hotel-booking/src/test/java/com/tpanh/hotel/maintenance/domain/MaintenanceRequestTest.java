package com.tpanh.hotel.maintenance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tpanh.hotel.maintenance.exception.InvalidMaintenanceRequestTransitionException;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.user.domain.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MaintenanceRequestTest {

    private static final Instant STARTED_AT = Instant.parse("2026-09-04T02:00:00Z");
    private static final Instant COMPLETED_AT = Instant.parse("2026-09-04T04:00:00Z");

    @Test
    void createStartsPendingWithoutWorkflowTimestamps() {
        Room room = room(RoomStatus.MAINTENANCE);
        User manager = User.builder().fullName("Manager").build();

        MaintenanceRequest request = MaintenanceRequest.create(room, "Điều hòa hỏng", "Không làm lạnh", manager);

        assertThat(request.getRoom()).isSameAs(room);
        assertThat(request.getCreatedBy()).isSameAs(manager);
        assertThat(request.getTitle()).isEqualTo("Điều hòa hỏng");
        assertThat(request.getDescription()).isEqualTo("Không làm lạnh");
        assertThat(request.getStatus()).isEqualTo(MaintenanceRequestStatus.PENDING);
        assertThat(request.getStartedAt()).isNull();
        assertThat(request.getCompletedAt()).isNull();
    }

    @Test
    void transitionsPendingToInProgressAndRecordsStartTime() {
        MaintenanceRequest request = pendingRequest();

        request.transitionTo(MaintenanceRequestStatus.IN_PROGRESS, STARTED_AT);

        assertThat(request.getStatus()).isEqualTo(MaintenanceRequestStatus.IN_PROGRESS);
        assertThat(request.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(request.getCompletedAt()).isNull();
    }

    @Test
    void transitionsInProgressToCompletedAndKeepsStartTime() {
        MaintenanceRequest request = pendingRequest();
        request.transitionTo(MaintenanceRequestStatus.IN_PROGRESS, STARTED_AT);

        request.transitionTo(MaintenanceRequestStatus.COMPLETED, COMPLETED_AT);

        assertThat(request.getStatus()).isEqualTo(MaintenanceRequestStatus.COMPLETED);
        assertThat(request.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(request.getCompletedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    void rejectsSkippedRepeatedAndBackwardTransitionsWithoutChangingTimestamps() {
        MaintenanceRequest pending = pendingRequest();
        assertThatThrownBy(() -> pending.transitionTo(MaintenanceRequestStatus.COMPLETED, COMPLETED_AT))
                .isInstanceOf(InvalidMaintenanceRequestTransitionException.class)
                .extracting("errorCode")
                .isEqualTo("MAINTENANCE_REQUEST_STATUS_CONFLICT");
        assertThat(pending.getStatus()).isEqualTo(MaintenanceRequestStatus.PENDING);
        assertThat(pending.getStartedAt()).isNull();
        assertThat(pending.getCompletedAt()).isNull();

        pending.transitionTo(MaintenanceRequestStatus.IN_PROGRESS, STARTED_AT);
        assertThatThrownBy(() -> pending.transitionTo(MaintenanceRequestStatus.IN_PROGRESS, COMPLETED_AT))
                .isInstanceOf(InvalidMaintenanceRequestTransitionException.class);
        assertThat(pending.getStartedAt()).isEqualTo(STARTED_AT);

        pending.transitionTo(MaintenanceRequestStatus.COMPLETED, COMPLETED_AT);
        assertThatThrownBy(
                        () -> pending.transitionTo(MaintenanceRequestStatus.IN_PROGRESS, COMPLETED_AT.plusSeconds(1)))
                .isInstanceOf(InvalidMaintenanceRequestTransitionException.class);
        assertThat(pending.getStatus()).isEqualTo(MaintenanceRequestStatus.COMPLETED);
        assertThat(pending.getCompletedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    void rejectsNullRequiredCreationAndTransitionValues() {
        Room room = room(RoomStatus.MAINTENANCE);
        User manager = User.builder().fullName("Manager").build();

        assertThatThrownBy(() -> MaintenanceRequest.create(null, "Title", null, manager))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Phòng không được để trống");
        assertThatThrownBy(() -> MaintenanceRequest.create(room, null, null, manager))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Tiêu đề không được để trống");
        assertThatThrownBy(() -> MaintenanceRequest.create(room, "Title", null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Người tạo không được để trống");

        MaintenanceRequest request = pendingRequest();
        assertThatThrownBy(() -> request.transitionTo(null, STARTED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Trạng thái mới không được để trống");
        assertThatThrownBy(() -> request.transitionTo(MaintenanceRequestStatus.IN_PROGRESS, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Thời điểm thay đổi không được để trống");
    }

    private MaintenanceRequest pendingRequest() {
        return MaintenanceRequest.create(
                room(RoomStatus.MAINTENANCE),
                "Điều hòa hỏng",
                null,
                User.builder().fullName("Manager").build());
    }

    private Room room(RoomStatus status) {
        return Room.builder().roomNumber("0501").status(status).build();
    }
}
