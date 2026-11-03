package com.tpanh.hotel.housekeeping.service;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.housekeeping.dto.response.DirtyRoomResponse;
import java.util.UUID;

public interface HousekeepingService {

    PageResponse<DirtyRoomResponse> getDirtyRooms(int page, int size);

    PageResponse<DirtyRoomResponse> getRoomsRequiringCleaning(int page, int size);

    DirtyRoomResponse startCleaning(UUID roomId);

    DirtyRoomResponse completeCleaning(UUID roomId);
}
