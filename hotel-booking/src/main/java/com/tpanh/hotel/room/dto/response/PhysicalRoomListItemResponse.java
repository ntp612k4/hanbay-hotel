package com.tpanh.hotel.room.dto.response;

import com.tpanh.hotel.room.domain.RoomStatus;
import java.util.UUID;

public record PhysicalRoomListItemResponse(UUID roomId, String roomNumber, int floor, RoomStatus status) {}
