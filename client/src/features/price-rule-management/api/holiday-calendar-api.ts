import { apiClient } from '../../../lib/axios';
import type { ApiResponse, PageResponse } from '../../../types/api';

export interface HolidayValues {
  name: string;
  startDate: string;
  endDate: string;
}
export interface HolidayCalendarItem extends HolidayValues {
  id: string;
  active: boolean;
}
const path = '/management/holiday-calendar';

export async function getHolidayCalendar(page: number, active?: boolean) {
  const response = await apiClient.get<ApiResponse<PageResponse<HolidayCalendarItem>>>(path, {
    params: {
      page,
      size: 100,
      sort: 'startDate,desc',
      ...(active === undefined ? {} : { active }),
    },
  });
  return response.data.data;
}

export async function getActiveHolidays() {
  const items: HolidayCalendarItem[] = [];
  let page = 0;
  while (true) {
    const result = await getHolidayCalendar(page++, true);
    items.push(...result.content);
    if (result.last) return items;
  }
}

export async function saveHoliday(values: HolidayValues, id?: string) {
  if (id) await apiClient.put(`${path}/${id}`, values);
  else await apiClient.post(path, values);
}

export async function setHolidayStatus(id: string, active: boolean) {
  await apiClient.patch(`${path}/${id}/status`, { active });
}
