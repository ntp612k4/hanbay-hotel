// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App } from 'antd';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useRoomTypeCatalog } from '../../room/api/room-type-query';
import { useManagedPriceRules, usePriceRuleMutations } from '../hooks/use-price-rule-management';
import { PriceRuleManagementPage } from './price-rule-management-page';

vi.mock('../../room/api/room-type-query', () => ({
  useRoomTypeCatalog: vi.fn(),
}));

vi.mock('../hooks/use-price-rule-management', () => ({
  useManagedPriceRules: vi.fn(),
  usePriceRuleMutations: vi.fn(),
}));

vi.mock('../components/price-rule-form-modal', () => ({
  PriceRuleFormModal: ({ open, editing }: { open: boolean; editing: { name: string } | null }) =>
    open ? (
      <div>
        <span>{editing ? 'Cập nhật quy tắc giá' : 'Thêm quy tắc giá'}</span>
        {editing && <input readOnly value={editing.name} />}
      </div>
    ) : null,
}));

const refetch = vi.fn();
const createMutation = { mutate: vi.fn(), isPending: false };
const updateMutation = { mutate: vi.fn(), isPending: false };
const statusMutation = { mutateAsync: vi.fn(), isPending: false, variables: undefined };

const item = {
  id: 'rule-id',
  roomType: {
    id: 'room-type-id',
    code: 'DLX',
    name: 'Deluxe',
  },
  name: 'Weekend surcharge Deluxe',
  ruleType: 'WEEKEND',
  adjustmentType: 'PERCENTAGE',
  adjustmentValue: 20,
  adjustmentDirection: 'INCREASE' as const,
  startDate: '2026-09-01',
  endDate: '2026-12-31',
  priority: 10,
  active: true,
  createdAt: '2026-08-29T08:00:00Z',
  updatedAt: '2026-08-29T09:00:00Z',
};

const roomTypePage = {
  content: [
    {
      id: 'room-type-id',
      name: 'Deluxe',
      description: 'Deluxe room',
      capacityAdults: 2,
      capacityChildren: 1,
      basePrice: 1_500_000,
      thumbnailUrl: null,
    },
  ],
  page: 0,
  size: 100,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
};

beforeEach(() => {
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }));
  vi.clearAllMocks();
  vi.mocked(useRoomTypeCatalog).mockReturnValue({
    data: roomTypePage,
    isLoading: false,
  } as never);
  vi.mocked(usePriceRuleMutations).mockReturnValue({
    createMutation,
    updateMutation,
    statusMutation,
  } as never);
});

afterEach(cleanup);

function renderPage() {
  return render(
    <App>
      <PriceRuleManagementPage />
    </App>,
  );
}

describe('PriceRuleManagementPage', () => {
  function mockCatalog() {
    vi.mocked(useManagedPriceRules).mockReturnValue({
      data: {
        content: [item],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      },
      isLoading: false,
      isError: false,
      refetch,
    } as never);
  }

  it('renders management data and the create action', () => {
    mockCatalog();

    renderPage();

    expect(screen.getByText('Weekend surcharge Deluxe')).toBeTruthy();
    expect(screen.getByText('Deluxe')).toBeTruthy();
    expect(screen.getByText('Cuối tuần')).toBeTruthy();
    expect(screen.getByText('+20%')).toBeTruthy();
    expect(screen.getByText('Đang hoạt động')).toBeTruthy();

    expect(screen.getByRole('button', { name: /Thêm quy tắc/ })).toBeTruthy();
  });

  it('shows an empty state when no price rules exist', () => {
    vi.mocked(useManagedPriceRules).mockReturnValue({
      data: {
        content: [],
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
      },
      isLoading: false,
      isError: false,
      refetch,
    } as never);

    renderPage();

    expect(screen.getByText('Chưa có quy tắc giá')).toBeTruthy();
  });

  it('shows an error state and retries the list request', () => {
    vi.mocked(useManagedPriceRules).mockReturnValue({
      error: new Error('Network error'),
      isLoading: false,
      isError: true,
      refetch,
    } as never);

    renderPage();

    expect(screen.getByText('Không tải được danh mục quy tắc giá')).toBeTruthy();
    expect(
      screen.getByText('Không thể kết nối đến hệ thống. Vui lòng kiểm tra kết nối và thử lại.'),
    ).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }));
    expect(refetch).toHaveBeenCalledOnce();
  });

  it('passes trimmed search and filters to the query hook', () => {
    vi.mocked(useManagedPriceRules).mockReturnValue({
      data: { content: [], totalElements: 0 },
      isLoading: false,
      isError: false,
      refetch,
    } as never);

    renderPage();
    fireEvent.change(screen.getByLabelText('Tìm quy tắc giá'), {
      target: { value: '  weekend  ' },
    });
    fireEvent.keyDown(screen.getByLabelText('Tìm quy tắc giá'), { key: 'Enter' });
    fireEvent.mouseDown(screen.getByLabelText('Lọc hạng phòng'));
    fireEvent.click(screen.getByText('Deluxe'));
    fireEvent.mouseDown(screen.getByLabelText('Lọc loại quy tắc'));
    fireEvent.click(screen.getByText('Khuyến mãi công khai'));
    fireEvent.mouseDown(screen.getByLabelText('Lọc trạng thái quy tắc giá'));
    fireEvent.click(screen.getByText('Ngừng hoạt động'));

    const calls = vi.mocked(useManagedPriceRules).mock.calls;
    expect(calls.some(([params]) => params.search === 'weekend')).toBe(true);
    expect(calls.some(([params]) => params.roomTypeId === 'room-type-id')).toBe(true);
    expect(calls.some(([params]) => params.ruleType === 'PROMOTION')).toBe(true);
    expect(calls.some(([params]) => params.active === false)).toBe(true);
  });

  it('returns to the last existing page when the filtered catalog shrinks', async () => {
    vi.mocked(useManagedPriceRules).mockReturnValue({
      data: {
        content: [item],
        page: 0,
        size: 10,
        totalElements: 11,
        totalPages: 2,
        first: true,
        last: false,
      },
      isLoading: false,
      isError: false,
      refetch,
    } as never);

    const { rerender } = renderPage();
    const nextButton = document.querySelector<HTMLButtonElement>('.ant-pagination-next button');
    expect(nextButton).not.toBeNull();
    fireEvent.click(nextButton!);

    vi.mocked(useManagedPriceRules).mockReturnValue({
      data: {
        content: [],
        page: 1,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        first: false,
        last: true,
      },
      isLoading: false,
      isError: false,
      refetch,
    } as never);
    rerender(
      <App>
        <PriceRuleManagementPage />
      </App>,
    );

    await waitFor(() => {
      expect(useManagedPriceRules).toHaveBeenLastCalledWith(
        expect.objectContaining({ page: 0, size: 10 }),
      );
    });
  });
});
