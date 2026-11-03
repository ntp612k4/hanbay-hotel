package com.tpanh.hotel.pricing.service;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.pricing.service.model.CreateHolidayCalendarCommand;
import com.tpanh.hotel.pricing.service.model.HolidayCalendarManagementResult;
import com.tpanh.hotel.pricing.service.model.HolidayCalendarSearchCriteria;
import com.tpanh.hotel.pricing.service.model.UpdateHolidayCalendarCommand;
import com.tpanh.hotel.pricing.service.model.UpdateHolidayCalendarStatusCommand;
import java.util.UUID;

public interface HolidayCalendarManagementService {

    PageResponse<HolidayCalendarManagementResult> findAll(HolidayCalendarSearchCriteria criteria);

    HolidayCalendarManagementResult findById(UUID id);

    HolidayCalendarManagementResult create(CreateHolidayCalendarCommand command);

    HolidayCalendarManagementResult update(UUID id, UpdateHolidayCalendarCommand command);

    HolidayCalendarManagementResult updateStatus(UUID id, UpdateHolidayCalendarStatusCommand command);
}
