package com.tpanh.hotel.pricing.service.model;

import com.tpanh.hotel.pricing.domain.RuleType;
import java.util.UUID;

public record PriceRuleSearchCriteria(
        int page,
        int size,
        String search,
        UUID roomTypeId,
        RuleType ruleType,
        Boolean active,
        String sortBy,
        String direction) {}
