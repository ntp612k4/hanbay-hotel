import { apiClient } from '../../../lib/axios';

import type {
  CalculateStayPriceApiResponse,
  CalculateStayPriceRequest,
  CalculateStayPriceResponse,
} from '../types/stay-price';

export async function calculateStayPrice(
  request: CalculateStayPriceRequest,
  signal?: AbortSignal,
): Promise<CalculateStayPriceResponse> {
  const response = await apiClient.post<CalculateStayPriceApiResponse>(
    '/pricing/stay-price-calculations',
    request,
    { signal },
  );

  return response.data.data;
}
