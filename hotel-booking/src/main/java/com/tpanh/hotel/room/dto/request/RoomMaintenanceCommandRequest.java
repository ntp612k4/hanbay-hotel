package com.tpanh.hotel.room.dto.request;

import com.tpanh.hotel.room.domain.RoomMaintenanceAction;
import jakarta.validation.constraints.NotNull;

public record RoomMaintenanceCommandRequest(
        @NotNull(message = "Hành động bảo trì không được để trống") RoomMaintenanceAction action) {}
