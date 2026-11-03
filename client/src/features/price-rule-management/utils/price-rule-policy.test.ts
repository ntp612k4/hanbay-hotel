import { describe, expect, it } from 'vitest';

import { allowsDirectionSelection, directionForRuleType } from './price-rule-policy';

describe('price rule policy', () => {
  it('keeps the selected direction for a seasonal rule', () => {
    expect(directionForRuleType('SEASONAL', 'DISCOUNT')).toBe('DISCOUNT');
    expect(allowsDirectionSelection('SEASONAL')).toBe(true);
  });

  it('enforces the fixed direction when the rule type changes', () => {
    expect(directionForRuleType('WEEKEND', 'DISCOUNT')).toBe('INCREASE');
    expect(directionForRuleType('HOLIDAY', 'DISCOUNT')).toBe('INCREASE');
    expect(directionForRuleType('PROMOTION', 'INCREASE')).toBe('DISCOUNT');
    expect(allowsDirectionSelection('WEEKEND')).toBe(false);
  });
});
