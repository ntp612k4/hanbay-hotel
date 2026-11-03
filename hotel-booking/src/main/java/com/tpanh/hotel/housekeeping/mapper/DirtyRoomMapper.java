package com.tpanh.hotel.housekeeping.mapper;

import com.tpanh.hotel.common.mapper.MapperConfig;
import com.tpanh.hotel.housekeeping.dto.response.DirtyRoomResponse;
import com.tpanh.hotel.room.domain.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface DirtyRoomMapper {

    @Mapping(target = "roomId", source = "id")
    DirtyRoomResponse toResponse(Room room);
}
