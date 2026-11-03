package com.tpanh.hotel.maintenance.domain;

import com.tpanh.hotel.common.domain.BaseEntity;
import com.tpanh.hotel.maintenance.exception.InvalidMaintenanceRequestTransitionException;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.user.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "maintenance_requests",
        indexes = {@Index(name = "idx_maintenance_requests_created_at", columnList = "created_at")})
public class MaintenanceRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, updatable = false)
    private Room room;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private MaintenanceRequestStatus status = MaintenanceRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static MaintenanceRequest create(Room room, String title, String description, User createdBy) {
        return MaintenanceRequest.builder()
                .room(Objects.requireNonNull(room, "Phòng không được để trống"))
                .title(Objects.requireNonNull(title, "Tiêu đề không được để trống"))
                .description(description)
                .createdBy(Objects.requireNonNull(createdBy, "Người tạo không được để trống"))
                .status(MaintenanceRequestStatus.PENDING)
                .build();
    }

    public void transitionTo(MaintenanceRequestStatus newStatus, Instant changedAt) {
        Objects.requireNonNull(newStatus, "Trạng thái mới không được để trống");
        Objects.requireNonNull(changedAt, "Thời điểm thay đổi không được để trống");

        if (status == MaintenanceRequestStatus.PENDING && newStatus == MaintenanceRequestStatus.IN_PROGRESS) {
            status = newStatus;
            startedAt = changedAt;
            return;
        }
        if (status == MaintenanceRequestStatus.IN_PROGRESS && newStatus == MaintenanceRequestStatus.COMPLETED) {
            status = newStatus;
            completedAt = changedAt;
            return;
        }
        throw new InvalidMaintenanceRequestTransitionException(status, newStatus);
    }
}
