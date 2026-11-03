import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getActiveHolidays,
  getHolidayCalendar,
  saveHoliday,
  setHolidayStatus,
  type HolidayValues,
} from '../api/holiday-calendar-api';

export function useHolidayCalendar(page: number) {
  return useQuery({
    queryKey: ['holiday-calendar', 'list', page],
    queryFn: () => getHolidayCalendar(page),
  });
}
export function useActiveHolidays(enabled: boolean) {
  return useQuery({
    queryKey: ['holiday-calendar', 'active'],
    queryFn: getActiveHolidays,
    enabled,
  });
}
export function useHolidayMutations() {
  const client = useQueryClient();
  const refresh = async () => {
    await Promise.all(
      ['holiday-calendar', 'available-room-search', 'stay-price'].map((key) =>
        client.invalidateQueries({ queryKey: [key] }),
      ),
    );
  };
  return {
    save: useMutation({
      mutationFn: ({ values, id }: { values: HolidayValues; id?: string }) =>
        saveHoliday(values, id),
      onSuccess: refresh,
    }),
    status: useMutation({
      mutationFn: ({ id, active }: { id: string; active: boolean }) => setHolidayStatus(id, active),
      onSuccess: refresh,
    }),
  };
}
