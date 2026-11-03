import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from '@tanstack/react-query';

import type {
  CheckOutGuestRequest,
  EarlyCheckoutRequest,
  TodayDepartureParams,
} from '../types/today-departure';
import {
  checkOutGuest,
  earlyCheckout,
  getTodayDepartures,
  quoteEarlyCheckout,
} from './today-departure-api';

const invalidateCheckoutData = async (queryClient: QueryClient) => {
  await Promise.all(
    [
      'today-departures',
      'housekeeping',
      'manager-dashboard',
      'payment-monitoring',
      'booking-history',
      'booking-detail',
      'refund-requests',
      'early-checkout-quote',
    ].map((key) => queryClient.invalidateQueries({ queryKey: [key] })),
  );
};

export const todayDepartureKeys = {
  all: ['today-departures'] as const,
  list: (params: TodayDepartureParams) => [...todayDepartureKeys.all, params] as const,
};

export const useEarlyCheckoutQuote = (
  bookingId: string | undefined,
  actualCheckOutDate: string | undefined,
) =>
  useQuery({
    queryKey: ['early-checkout-quote', bookingId, actualCheckOutDate],
    queryFn: () => quoteEarlyCheckout(bookingId!, actualCheckOutDate!),
    enabled: Boolean(bookingId && actualCheckOutDate),
    retry: false,
  });

export const useEarlyCheckout = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ bookingId, payload }: { bookingId: string; payload: EarlyCheckoutRequest }) =>
      earlyCheckout(bookingId, payload),
    onSettled: () => invalidateCheckoutData(queryClient),
  });
};

export const useTodayDepartures = (params: TodayDepartureParams) =>
  useQuery({
    queryKey: todayDepartureKeys.list(params),
    queryFn: () => getTodayDepartures(params),
    placeholderData: keepPreviousData,
  });

interface CheckOutGuestMutationInput {
  bookingId: string;
  payload?: CheckOutGuestRequest;
}

export const useCheckOutGuest = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ bookingId, payload }: CheckOutGuestMutationInput) =>
      checkOutGuest(bookingId, payload),
    onSettled: () => invalidateCheckoutData(queryClient),
  });
};
