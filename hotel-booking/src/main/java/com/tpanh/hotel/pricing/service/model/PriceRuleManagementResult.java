package com.tpanh.hotel.pricing.service.model;

import com.tpanh.hotel.pricing.domain.AdjustmentDirection;
import com.tpanh.hotel.pricing.domain.AdjustmentType;
import com.tpanh.hotel.pricing.domain.RuleType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PriceRuleManagementResult(
        UUID id,
        RoomTypeSummaryResult roomType,
        String name,
        RuleType ruleType,
        AdjustmentType adjustmentType,
        long adjustmentValue,
        AdjustmentDirection adjustmentDirection,
        LocalDate startDate,
        LocalDate endDate,
        int priority,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public PriceRuleManagementResult(
            UUID id,
            RoomTypeSummaryResult roomType,
            String name,
            RuleType ruleType,
            AdjustmentType adjustmentType,
            long adjustmentValue,
            LocalDate startDate,
            LocalDate endDate,
            int priority,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        this(
                id,
                roomType,
                name,
                ruleType,
                adjustmentType,
                adjustmentValue,
                ruleType == RuleType.PROMOTION ? AdjustmentDirection.DISCOUNT : AdjustmentDirection.INCREASE,
                startDate,
                endDate,
                priority,
                active,
                createdAt,
                updatedAt);
    }
}
