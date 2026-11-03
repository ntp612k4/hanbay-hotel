package com.tpanh.hotel.addon.mapper;

import com.tpanh.hotel.addon.domain.AddOnService;
import com.tpanh.hotel.addon.dto.response.AddOnCatalogResponse;
import com.tpanh.hotel.addon.dto.response.AddOnManagementResponse;
import com.tpanh.hotel.addon.service.model.AddOnManagementResult;
import com.tpanh.hotel.common.mapper.MapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface AddOnMapper {
    AddOnCatalogResponse toCatalogResponse(AddOnService service);

    AddOnManagementResponse toManagementResponse(AddOnManagementResult result);
}
