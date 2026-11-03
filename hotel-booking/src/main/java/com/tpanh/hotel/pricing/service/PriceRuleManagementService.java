package com.tpanh.hotel.pricing.service;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.pricing.service.model.CreatePriceRuleCommand;
import com.tpanh.hotel.pricing.service.model.PriceRuleManagementResult;
import com.tpanh.hotel.pricing.service.model.PriceRuleSearchCriteria;
import com.tpanh.hotel.pricing.service.model.UpdatePriceRuleCommand;
import com.tpanh.hotel.pricing.service.model.UpdatePriceRuleStatusCommand;
import java.util.UUID;

public interface PriceRuleManagementService {

    PageResponse<PriceRuleManagementResult> findAll(PriceRuleSearchCriteria criteria);

    PriceRuleManagementResult findById(UUID id);

    PriceRuleManagementResult create(CreatePriceRuleCommand command);

    PriceRuleManagementResult update(UUID id, UpdatePriceRuleCommand command);

    PriceRuleManagementResult updateStatus(UUID id, UpdatePriceRuleStatusCommand command);
}
