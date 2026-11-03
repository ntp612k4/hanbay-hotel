package com.tpanh.hotel.pricing.mapper;

import com.tpanh.hotel.common.mapper.MapperConfig;
import com.tpanh.hotel.pricing.dto.response.CalculateStayPriceResponse;
import com.tpanh.hotel.pricing.dto.response.PriceRuleManagementResponse;
import com.tpanh.hotel.pricing.dto.response.RoomTypePriceResponse;
import com.tpanh.hotel.pricing.dto.response.RoomTypeSummaryResponse;
import com.tpanh.hotel.pricing.dto.response.StayNightPriceResponse;
import com.tpanh.hotel.pricing.dto.result.NightlyPriceResult;
import com.tpanh.hotel.pricing.dto.result.RoomTypePricingResult;
import com.tpanh.hotel.pricing.dto.result.StayPriceResult;
import com.tpanh.hotel.pricing.service.model.PriceRuleManagementResult;
import com.tpanh.hotel.pricing.service.model.RoomTypeSummaryResult;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface PricingMapper {
    CalculateStayPriceResponse toResponse(StayPriceResult result);

    RoomTypePriceResponse toRoomTypeResponse(RoomTypePricingResult result);

    StayNightPriceResponse toNightlyResponse(NightlyPriceResult result);

    PriceRuleManagementResponse toManagementResponse(PriceRuleManagementResult result);

    RoomTypeSummaryResponse toRoomTypeSummaryResponse(RoomTypeSummaryResult result);
}
