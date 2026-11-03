package com.tpanh.hotel.pricing.service.model;

import com.tpanh.hotel.pricing.domain.AdjustmentDirection;
import com.tpanh.hotel.pricing.domain.AdjustmentType;
import com.tpanh.hotel.pricing.domain.RuleType;
import java.time.LocalDate;
import java.util.UUID;

public record UpdatePriceRuleCommand(
        UUID roomTypeId,
        String name,
        RuleType ruleType,
        AdjustmentType adjustmentType,
        Long adjustmentValue,
        AdjustmentDirection adjustmentDirection,
        LocalDate startDate,
        LocalDate endDate,
        Integer priority) {

    public UpdatePriceRuleCommand(
            UUID roomTypeId,
            String name,
            RuleType ruleType,
            AdjustmentType adjustmentType,
            Long adjustmentValue,
            LocalDate startDate,
            LocalDate endDate,
            Integer priority) {
        this(roomTypeId, name, ruleType, adjustmentType, adjustmentValue, null, startDate, endDate, priority);
    }
}
