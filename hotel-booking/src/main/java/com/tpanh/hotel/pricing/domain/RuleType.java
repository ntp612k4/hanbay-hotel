package com.tpanh.hotel.pricing.domain;

public enum RuleType {
    BASE(0),
    SEASONAL(10),
    WEEKEND(20),
    HOLIDAY(30),
    PROMOTION(40);

    private final int priority;

    RuleType(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
