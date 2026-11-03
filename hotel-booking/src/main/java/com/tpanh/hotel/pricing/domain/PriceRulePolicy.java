package com.tpanh.hotel.pricing.domain;

public final class PriceRulePolicy {

    private PriceRulePolicy() {}

    public static AdjustmentDirection defaultDirection(RuleType ruleType) {
        return ruleType == RuleType.PROMOTION ? AdjustmentDirection.DISCOUNT : AdjustmentDirection.INCREASE;
    }

    public static boolean supportsDirection(RuleType ruleType, AdjustmentDirection adjustmentDirection) {
        if (ruleType == null || adjustmentDirection == null) {
            return false;
        }
        return switch (ruleType) {
            case PROMOTION -> adjustmentDirection == AdjustmentDirection.DISCOUNT;
            case WEEKEND, HOLIDAY -> adjustmentDirection == AdjustmentDirection.INCREASE;
            case SEASONAL, BASE -> true;
        };
    }

    public static String directionConstraintMessage(RuleType ruleType) {
        return switch (ruleType) {
            case PROMOTION -> "Khuyến mãi chỉ hỗ trợ hướng giảm giá.";
            case WEEKEND, HOLIDAY -> "Quy tắc cuối tuần và ngày lễ chỉ hỗ trợ hướng tăng giá.";
            case SEASONAL, BASE -> "Hướng điều chỉnh không hợp lệ.";
        };
    }
}
