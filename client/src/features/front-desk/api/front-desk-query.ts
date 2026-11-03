import { keepPreviousData, useQuery } from '@tanstack/react-query';

import type { TodayArrivalsParams } from '../types/today-arrivals';
import { getTodayArrivals } from './front-desk-api';

export const frontDeskKeys = {
  all: ['front-desk'] as const,
  todayArrivals: (params: TodayArrivalsParams) =>
    [...frontDeskKeys.all, 'today-arrivals', params] as const,
};

export const useTodayArrivals = (params: TodayArrivalsParams) =>
  useQuery({
    queryKey: frontDeskKeys.todayArrivals(params),
    queryFn: () => getTodayArrivals(params),
    placeholderData: keepPreviousData,
  });
