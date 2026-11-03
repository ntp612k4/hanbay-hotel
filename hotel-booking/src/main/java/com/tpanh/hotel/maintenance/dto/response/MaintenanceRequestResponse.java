package com.tpanh.hotel.maintenance.dto.response;

import com.tpanh.hotel.maintenance.domain.MaintenanceRequestStatus;
import com.tpanh.hotel.room.domain.RoomStatus;
import java.time.Instant;
import java.util.UUID;

public record MaintenanceRequestResponse(
        UUID id,
        UUID roomId,
        String roomNumber,
        RoomStatus roomStatus,
        String title,
        String description,
        MaintenanceRequestStatus status,
        UUID createdById,
        String createdByName,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant completedAt) {}
