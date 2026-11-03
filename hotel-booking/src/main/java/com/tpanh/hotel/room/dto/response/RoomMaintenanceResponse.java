package com.tpanh.hotel.room.dto.response;

import com.tpanh.hotel.room.domain.RoomStatus;
import java.util.UUID;

public record RoomMaintenanceResponse(UUID roomId, String roomNumber, RoomStatus status) {}
