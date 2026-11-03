package com.tpanh.hotel.room.service;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.room.dto.RoomTypeCatalogItemResponse;
import com.tpanh.hotel.room.dto.RoomTypeDetailResponse;
import java.util.UUID;

public interface RoomTypeService {

    PageResponse<RoomTypeCatalogItemResponse> getCatalog(
            int page, int size, String keyword, String sortBy, String direction);

    RoomTypeDetailResponse getDetail(UUID id);

    RoomTypeDetailResponse getDetailByName(String name);
}
