// @vitest-environment jsdom

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { ApiClientError } from '../../../types/api';
import { calculateStayPrice } from '../api/stay-price-api';
import type { CalculateStayPriceResponse } from '../types/stay-price';
import { useStayPrice } from './use-stay-price';

vi.mock('../api/stay-price-api', () => ({ calculateStayPrice: vi.fn() }));

const roomTypeId = '10000000-0000-0000-0000-000000000001';

afterEach(() => {
  vi.clearAllMocks();
});

describe('useStayPrice', () => {
  it('không gọi API khi chưa đủ dữ liệu hợp lệ', () => {
    const queryClient = createQueryClient();
    const { unmount } = renderHook(() => useStayPrice(undefined), {
      wrapper: createWrapper(queryClient),
    });

    expect(calculateStayPrice).not.toHaveBeenCalled();
    unmount();
    queryClient.clear();
  });

  it('gọi lại theo selection mới và không hiển thị kết quả cũ', async () => {
    const first = deferred<CalculateStayPriceResponse>();
    const second = deferred<CalculateStayPriceResponse>();
    vi.mocked(calculateStayPrice)
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(second.promise);
    const queryClient = createQueryClient();
    const firstRequest = {
      roomTypeId,
      checkInDate: '2026-08-20',
      checkOutDate: '2026-08-22',
    };
    const secondRequest = {
      ...firstRequest,
      checkOutDate: '2026-08-23',
    };
    const { result, rerender, unmount } = renderHook(({ request }) => useStayPrice(request), {
      initialProps: { request: firstRequest },
      wrapper: createWrapper(queryClient),
    });

    await waitFor(() => expect(calculateStayPrice).toHaveBeenCalledTimes(1));
    rerender({ request: secondRequest });
    await waitFor(() => expect(calculateStayPrice).toHaveBeenCalledTimes(2));
    expect(result.current.data).toBeUndefined();

    second.resolve(responseFor(secondRequest.checkOutDate, 3, 3_300_000));
    await waitFor(() => expect(result.current.data?.totalPrice).toBe(3_300_000));
    first.resolve(responseFor(firstRequest.checkOutDate, 2, 2_000_000));
    await waitFor(() => expect(result.current.data?.checkOutDate).toBe('2026-08-23'));

    expect(vi.mocked(calculateStayPrice).mock.calls[1]?.[0]).toEqual(secondRequest);
    unmount();
    queryClient.clear();
  });

  it('không retry business error không thể tự phục hồi', async () => {
    vi.mocked(calculateStayPrice).mockRejectedValue(
      new ApiClientError({
        message: 'Không tìm thấy hạng phòng',
        kind: 'api',
        status: 404,
        code: 'ROOM_001',
      }),
    );
    const queryClient = createQueryClient();
    const request = {
      roomTypeId,
      checkInDate: '2026-08-20',
      checkOutDate: '2026-08-23',
    };
    const { result, unmount } = renderHook(() => useStayPrice(request), {
      wrapper: createWrapper(queryClient),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(calculateStayPrice).toHaveBeenCalledOnce();

    unmount();
    queryClient.clear();
  });
});

function createQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

function createWrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

function responseFor(checkOutDate: string, numberOfNights: number, totalPrice: number) {
  return {
    roomType: { id: roomTypeId, code: 'DLX', name: 'Deluxe' },
    checkInDate: '2026-08-20',
    checkOutDate,
    numberOfNights,
    nightlyPrices: [],
    totalPrice,
  };
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((promiseResolve) => {
    resolve = promiseResolve;
  });
  return { promise, resolve };
}
