// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  useMaintenanceDetail,
  useMaintenanceMutations,
  useMaintenanceRequests,
  usePhysicalRooms,
} from '../hooks/use-maintenance';
import { MaintenancePage } from './maintenance-page';

vi.mock('../components/maintenance-cost-panel', () => ({
  MaintenanceCostPanel: () => <div>Chi phí bảo trì</div>,
}));
vi.mock('../hooks/use-maintenance', () => ({
  useMaintenanceDetail: vi.fn(),
  useMaintenanceMutations: vi.fn(),
  useMaintenanceRequests: vi.fn(),
  usePhysicalRooms: vi.fn(),
}));
const messages = vi.hoisted(() => ({ success: vi.fn(), error: vi.fn() }));
vi.mock('antd', async () => {
  const actual = await vi.importActual<typeof import('antd')>('antd');
  return { ...actual, App: { ...actual.App, useApp: () => ({ message: messages }) } };
});
const create = vi.fn();
const updateStatus = vi.fn();
const refetch = vi.fn();
const rooms = [
  { roomId: 'room-1', roomNumber: '101', floor: 1, status: 'AVAILABLE' },
  { roomId: 'room-2', roomNumber: '102', floor: 1, status: 'OCCUPIED' },
];
const pending = {
  id: 'req-1',
  roomNumber: '101',
  title: 'Sửa đèn',
  status: 'PENDING',
  createdAt: '2026-09-08T03:00:00Z',
};
const query = (data: unknown) => ({
  data,
  isLoading: false,
  isFetching: false,
  isError: false,
  refetch,
});
beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(usePhysicalRooms).mockReturnValue(query(rooms) as never);
  vi.mocked(useMaintenanceRequests).mockReturnValue(
    query({ content: [pending], totalElements: 100 }) as never,
  );
  vi.mocked(useMaintenanceDetail).mockReturnValue(query(undefined) as never);
  vi.mocked(useMaintenanceMutations).mockReturnValue({
    create: { mutate: create, isPending: false },
    status: { mutate: updateStatus, isPending: false },
  } as never);
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
  window.matchMedia = vi.fn().mockImplementation(() => ({
    matches: false,
    addListener() {},
    removeListener() {},
    addEventListener() {},
    removeEventListener() {},
  }));
});
afterEach(cleanup);
const openCreate = () =>
  fireEvent.click(screen.getByRole('button', { name: /Tạo yêu cầu bảo trì/ }));

describe('MaintenancePage', () => {
  it('shows Vietnamese validation and handles invalid submission', async () => {
    render(<MaintenancePage />);
    openCreate();
    fireEvent.click(screen.getByRole('button', { name: 'Tạo yêu cầu' }));
    await waitFor(() => expect(screen.getByText('Vui lòng nhập tiêu đề bảo trì')).not.toBeNull());
    expect(create).not.toHaveBeenCalled();
  });
  it('only offers available rooms and submits a trimmed reason', async () => {
    render(<MaintenancePage />);
    openCreate();
    fireEvent.mouseDown(screen.getByRole('combobox'));
    fireEvent.click(screen.getAllByText('Phòng 101 - Tầng 1').at(-1)!);
    expect(screen.queryByText('Phòng 102 - Tầng 1')).toBeNull();
    fireEvent.change(screen.getByLabelText('Tiêu đề'), { target: { value: '  Điều hòa hỏng  ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Tạo yêu cầu' }));
    await waitFor(() =>
      expect(create).toHaveBeenCalledWith(
        { roomId: 'room-1', title: 'Điều hòa hỏng', description: undefined },
        expect.any(Object),
      ),
    );
  });
  it('allows retrying failed room loading and blocks create', () => {
    vi.mocked(usePhysicalRooms).mockReturnValue({
      ...query(undefined),
      isError: true,
      error: new Error('Mất kết nối'),
    } as never);
    render(<MaintenancePage />);
    openCreate();
    expect(screen.getByRole('button', { name: 'Tạo yêu cầu' })).toHaveProperty('disabled', true);
    fireEvent.click(screen.getByRole('button', { name: 'Tải lại phòng' }));
    expect(refetch).toHaveBeenCalled();
  });
  it('prevents closing the form during creation', () => {
    vi.mocked(useMaintenanceMutations).mockReturnValue({
      create: { mutate: create, isPending: true },
      status: { mutate: updateStatus, isPending: false },
    } as never);
    render(<MaintenancePage />);
    openCreate();
    expect(screen.getByRole('button', { name: 'Hủy' })).toHaveProperty('disabled', true);
  });
  it('offers fixed page size and progresses the selected request only', () => {
    render(<MaintenancePage />);
    expect(screen.queryByRole('combobox')).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Bắt đầu xử lý' }));
    expect(updateStatus).toHaveBeenCalledWith(
      { id: 'req-1', status: 'IN_PROGRESS' },
      expect.any(Object),
    );
  });
});
