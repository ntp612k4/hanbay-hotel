package com.tpanh.hotel.room.service;

import com.tpanh.hotel.room.domain.RoomMaintenanceAction;
import com.tpanh.hotel.room.dto.response.RoomMaintenanceResponse;

public interface RoomMaintenanceService {
    RoomMaintenanceResponse changeMaintenanceStatus(String roomNumber, RoomMaintenanceAction action);
}
