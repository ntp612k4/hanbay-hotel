package com.tpanh.hotel.room.domain;

import com.tpanh.hotel.common.domain.BaseEntity;
import com.tpanh.hotel.room.exception.RoomStateConflictException;
import jakarta.persistence.*;
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
@Table(name = "rooms")
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @EqualsAndHashCode.Include
    @Column(name = "room_number", nullable = false, unique = true, length = 30)
    private String roomNumber;

    @Column(name = "floor")
    private int floor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private RoomStatus status;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    public void updateDetails(RoomType type, String roomNumber, int floor, String note) {
        if (status != RoomStatus.AVAILABLE)
            throw new com.tpanh.hotel.room.exception.RoomValidationException(
                    "Chỉ sửa phòng đang sẵn sàng. Hãy hoàn tất nghiệp vụ vận hành trước.");
        this.roomType = type;
        this.roomNumber = roomNumber;
        this.floor = floor;
        this.note = note;
    }

    public void occupy() {
        if (status != RoomStatus.AVAILABLE) {
            throw RoomStateConflictException.notAvailable();
        }
        status = RoomStatus.OCCUPIED;
    }

    public void markDirtyAfterCheckout() {
        if (status != RoomStatus.OCCUPIED) {
            throw RoomStateConflictException.notOccupied();
        }
        status = RoomStatus.DIRTY;
    }

    public void startCleaning() {
        if (status != RoomStatus.DIRTY) {
            throw RoomStateConflictException.cannotStartCleaning(status);
        }
        status = RoomStatus.CLEANING;
    }

    public void completeCleaning() {
        if (status != RoomStatus.CLEANING) {
            throw RoomStateConflictException.cannotCompleteCleaning(status);
        }
        status = RoomStatus.AVAILABLE;
    }

    public void startMaintenance() {
        if (status != RoomStatus.AVAILABLE) {
            throw RoomStateConflictException.cannotStartMaintenance(status);
        }
        status = RoomStatus.MAINTENANCE;
    }

    public void endMaintenance() {
        if (status != RoomStatus.MAINTENANCE) {
            throw RoomStateConflictException.notUnderMaintenance(status);
        }
        status = RoomStatus.AVAILABLE;
    }
}
