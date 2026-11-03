import { useQuery } from '@tanstack/react-query';

import { ApiClientError } from '../../../types/api';
import { calculateStayPrice } from '../api/stay-price-api';
import type { CalculateStayPriceRequest } from '../types/stay-price';

export const stayPriceKeys = {
  all: ['stay-price-calculations'] as const,
  calculation: (request?: CalculateStayPriceRequest) =>
    [
      ...stayPriceKeys.all,
      request?.roomTypeId ?? null,
      request?.checkInDate ?? null,
      request?.checkOutDate ?? null,
    ] as const,
};

export function useStayPrice(request?: CalculateStayPriceRequest) {
  return useQuery({
    queryKey: stayPriceKeys.calculation(request),
    queryFn: ({ signal }) => calculateStayPrice(request!, signal),
    enabled: Boolean(request),
    retry: (failureCount, error) => {
      if (failureCount >= 1) return false;
      if (!(error instanceof ApiClientError)) return true;
      return error.kind === 'network' || (error.status !== undefined && error.status >= 500);
    },
  });
}
