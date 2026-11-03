import type { Dayjs } from 'dayjs';

export interface BookingFormValues {
  roomTypeId: string;
  stayDates: [Dayjs, Dayjs];
  adults: number;
  children: number;
  fullName: string;
  email: string;
  phone?: string;
  identityNumber: string;
}
