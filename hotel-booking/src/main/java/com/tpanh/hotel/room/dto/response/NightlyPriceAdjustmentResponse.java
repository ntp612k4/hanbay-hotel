package com.tpanh.hotel.room.dto.response;

import com.tpanh.hotel.pricing.domain.AdjustmentDirection;
import com.tpanh.hotel.pricing.domain.AdjustmentType;
import com.tpanh.hotel.pricing.domain.RuleType;

public record NightlyPriceAdjustmentResponse(
        String name,
        RuleType ruleType,
        AdjustmentType adjustmentType,
        AdjustmentDirection adjustmentDirection,
        long adjustmentValue,
        long amount) {}
