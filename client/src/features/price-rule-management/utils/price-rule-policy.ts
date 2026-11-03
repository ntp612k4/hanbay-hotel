import type { AdjustmentDirection, RuleType } from '../types/price-rule-management';

export function directionForRuleType(
  ruleType: RuleType,
  currentDirection?: AdjustmentDirection,
): AdjustmentDirection {
  if (ruleType === 'PROMOTION') return 'DISCOUNT';
  if (ruleType === 'WEEKEND' || ruleType === 'HOLIDAY') return 'INCREASE';
  return currentDirection ?? 'INCREASE';
}

export function allowsDirectionSelection(ruleType: RuleType | undefined) {
  return ruleType === 'SEASONAL';
}
