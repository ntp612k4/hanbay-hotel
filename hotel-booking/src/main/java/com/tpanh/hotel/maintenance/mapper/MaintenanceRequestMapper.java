package com.tpanh.hotel.maintenance.mapper;

import com.tpanh.hotel.common.mapper.MapperConfig;
import com.tpanh.hotel.maintenance.domain.MaintenanceRequest;
import com.tpanh.hotel.maintenance.dto.response.MaintenanceRequestListItemResponse;
import com.tpanh.hotel.maintenance.dto.response.MaintenanceRequestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface MaintenanceRequestMapper {

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomNumber", source = "room.roomNumber")
    @Mapping(target = "roomStatus", source = "room.status")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    MaintenanceRequestResponse toResponse(MaintenanceRequest request);

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomNumber", source = "room.roomNumber")
    @Mapping(target = "roomStatus", source = "room.status")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    MaintenanceRequestListItemResponse toListItemResponse(MaintenanceRequest request);
}
