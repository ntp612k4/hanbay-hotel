package com.tpanh.hotel.room.dto.response;

import com.tpanh.hotel.room.domain.*;
import java.util.UUID;

public record RoomManagementResponse(
        UUID id, UUID roomTypeId, String roomTypeName, String roomNumber, int floor, RoomStatus status, String note) {
    public static RoomManagementResponse from(Room r) {
        return new RoomManagementResponse(
                r.getId(),
                r.getRoomType().getId(),
                r.getRoomType().getName(),
                r.getRoomNumber(),
                r.getFloor(),
                r.getStatus(),
                r.getNote());
    }
}
