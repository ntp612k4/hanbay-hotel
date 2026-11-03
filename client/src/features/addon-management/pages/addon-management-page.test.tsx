// @vitest-environment jsdom

import { App } from 'antd';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useAddOnMutations, useManagedAddOns } from '../hooks/use-addon-management';
import { AddOnManagementPage } from './addon-management-page';

vi.mock('../hooks/use-addon-management', () => ({
  useManagedAddOns: vi.fn(),
  useAddOnMutations: vi.fn(),
}));

const refetch = vi.fn();
const createMutation = { mutate: vi.fn(), isPending: false };
const updateMutation = { mutate: vi.fn(), isPending: false };
const statusMutation = { mutateAsync: vi.fn(), isPending: false, variables: undefined };

const item = {
  id: 'addon-id',
  code: 'AIRPORT_PICKUP',
  name: 'Đón sân bay',
  description: 'Xe riêng tối đa 4 khách',
  price: 350000,
  active: true,
  createdAt: '2026-08-20T01:00:00Z',
  updatedAt: '2026-08-20T02:00:00Z',
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
  vi.mocked(useAddOnMutations).mockReturnValue({
    createMutation,
    updateMutation,
    statusMutation,
  } as never);
});

afterEach(cleanup);

function renderPage() {
  return render(
    <App>
      <AddOnManagementPage />
    </App>,
  );
}

describe('AddOnManagementPage', () => {
  function mockCatalog() {
    vi.mocked(useManagedAddOns).mockReturnValue({
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

  it('renders management data and opens the create form', () => {
    mockCatalog();

    renderPage();

    expect(screen.getByText('AIRPORT_PICKUP')).toBeTruthy();
    expect(screen.getByText('Đón sân bay')).toBeTruthy();
    expect(screen.getByText('Đang hoạt động')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: /Thêm dịch vụ/ }));
    expect(screen.getByText('Thêm dịch vụ bổ sung')).toBeTruthy();
  });

  it('opens the edit form with an immutable business code', () => {
    mockCatalog();

    renderPage();

    fireEvent.click(screen.getByRole('button', { name: /Sửa/ }));
    expect(screen.getByText('Cập nhật dịch vụ bổ sung')).toBeTruthy();
    expect((screen.getByDisplayValue('AIRPORT_PICKUP') as HTMLInputElement).disabled).toBe(true);
  });

  it('shows an empty state when the catalog has no records', () => {
    vi.mocked(useManagedAddOns).mockReturnValue({
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

    expect(screen.getByText('Chưa có dịch vụ bổ sung')).toBeTruthy();
  });

  it('shows an error message and retries the list request', () => {
    vi.mocked(useManagedAddOns).mockReturnValue({
      error: new Error('Mất kết nối'),
      isLoading: false,
      isError: true,
      refetch,
    } as never);

    renderPage();

    expect(screen.getByText('Không tải được danh mục dịch vụ')).toBeTruthy();
    expect(
      screen.getByText('Không thể kết nối đến hệ thống. Vui lòng kiểm tra kết nối và thử lại.'),
    ).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }));
    expect(refetch).toHaveBeenCalledOnce();
  });

  it('passes trimmed search and active filter to the query hook', () => {
    vi.mocked(useManagedAddOns).mockReturnValue({
      data: { content: [], totalElements: 0 },
      isLoading: false,
      isError: false,
      refetch,
    } as never);

    renderPage();
    fireEvent.change(screen.getByLabelText('Tìm dịch vụ bổ sung'), {
      target: { value: '  airport  ' },
    });
    fireEvent.keyDown(screen.getByLabelText('Tìm dịch vụ bổ sung'), { key: 'Enter' });
    fireEvent.mouseDown(screen.getByLabelText('Lọc trạng thái dịch vụ'));
    fireEvent.click(screen.getByText('Ngừng hoạt động'));

    const calls = vi.mocked(useManagedAddOns).mock.calls;
    expect(calls.some(([params]) => params.search === 'airport')).toBe(true);
    expect(calls.some(([params]) => params.active === false)).toBe(true);
  });

  it('returns to the last existing page when the filtered catalog shrinks', async () => {
    vi.mocked(useManagedAddOns).mockReturnValue({
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

    vi.mocked(useManagedAddOns).mockReturnValue({
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
        <AddOnManagementPage />
      </App>,
    );

    await waitFor(() => {
      expect(useManagedAddOns).toHaveBeenLastCalledWith(
        expect.objectContaining({ page: 0, size: 10 }),
      );
    });
  });
});
