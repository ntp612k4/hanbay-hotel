package com.tpanh.hotel.booking.dto.response;

import com.tpanh.hotel.room.domain.RoomStatus;
import java.util.UUID;

public record AssignedRoomResponse(UUID roomId, String roomNumber, int floor, RoomStatus status) {}
