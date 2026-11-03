package com.tpanh.hotel.pricing.dto.response;

import com.tpanh.hotel.pricing.domain.AdjustmentDirection;
import com.tpanh.hotel.pricing.domain.AdjustmentType;
import com.tpanh.hotel.pricing.domain.RuleType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PriceRuleManagementResponse(
        UUID id,
        RoomTypeSummaryResponse roomType,
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
        Instant updatedAt) {}
