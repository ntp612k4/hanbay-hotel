package com.tpanh.hotel.room.service;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.room.dto.response.PhysicalRoomListItemResponse;

public interface RoomService {

    PageResponse<PhysicalRoomListItemResponse> findAll(int page, int size);
}
