import type { Dayjs } from 'dayjs';

import type { PageResponse } from '../../../types/api';

export type RuleType = 'WEEKEND' | 'HOLIDAY' | 'SEASONAL' | 'PROMOTION';
export type AdjustmentType = 'FIXED' | 'PERCENTAGE';
export type AdjustmentDirection = 'INCREASE' | 'DISCOUNT';

export interface RoomTypeSummary {
  id: string;
  code: string;
  name: string;
}

export interface PriceRuleManagementItem {
  id: string;
  roomType: RoomTypeSummary;
  name: string;
  ruleType: RuleType;
  adjustmentType: AdjustmentType;
  adjustmentValue: number;
  adjustmentDirection: AdjustmentDirection;
  startDate: string;
  endDate: string;
  priority: number;
  active: boolean;
  createdAt: string;
  updatedAt: string | null;
}

export type PriceRuleManagementPage = PageResponse<PriceRuleManagementItem>;

export interface PriceRuleListParams {
  page: number;
  size: number;
  search?: string;
  roomTypeId?: string;
  ruleType?: RuleType;
  active?: boolean;
  sort: string;
}

export interface CreatePriceRulePayload {
  roomTypeId: string;
  name: string;
  ruleType: RuleType;
  adjustmentType: AdjustmentType;
  adjustmentValue: number;
  adjustmentDirection: AdjustmentDirection;
  startDate: string;
  endDate: string;
  priority: number;
}

export type UpdatePriceRulePayload = CreatePriceRulePayload;

export interface PriceRuleFormValues {
  roomTypeId: string;
  name: string;
  ruleType: RuleType;
  adjustmentType: AdjustmentType;
  adjustmentValue: number;
  adjustmentDirection: AdjustmentDirection;
  startDate: Dayjs;
  endDate: Dayjs;
  priority: number;
}
