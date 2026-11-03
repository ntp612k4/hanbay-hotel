package com.tpanh.hotel.addon.service;

import com.tpanh.hotel.addon.service.model.AddOnManagementResult;
import com.tpanh.hotel.addon.service.model.AddOnSearchCriteria;
import com.tpanh.hotel.addon.service.model.CreateAddOnCommand;
import com.tpanh.hotel.addon.service.model.UpdateAddOnCommand;
import com.tpanh.hotel.addon.service.model.UpdateAddOnStatusCommand;
import com.tpanh.hotel.common.response.PageResponse;
import java.util.UUID;

public interface AddOnManagementService {

    PageResponse<AddOnManagementResult> findAll(AddOnSearchCriteria criteria);

    AddOnManagementResult findById(UUID id);

    AddOnManagementResult create(CreateAddOnCommand command);

    AddOnManagementResult update(UUID id, UpdateAddOnCommand command);

    AddOnManagementResult updateStatus(UUID id, UpdateAddOnStatusCommand command);
}
