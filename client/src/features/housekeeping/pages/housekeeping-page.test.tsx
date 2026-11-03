// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useDirtyRooms, useHousekeepingAction } from '../api/housekeeping-query';
import type { DirtyRoom } from '../types/dirty-rooms';
import { HousekeepingPage } from './housekeeping-page';

vi.mock('../api/housekeeping-query', () => ({
  useDirtyRooms: vi.fn(),
  useHousekeepingAction: vi.fn(),
}));

const refetch = vi.fn();
const mutate = vi.fn();
const dirtyRoom: DirtyRoom = {
  roomId: '10000000-0000-0000-0000-000000000001',
  roomNumber: 'DLX-101',
  floor: 1,
  status: 'DIRTY',
  note: 'Khách yêu cầu kiểm tra minibar',
  roomType: {
    id: '20000000-0000-0000-0000-000000000001',
    code: 'DLX',
    name: 'Deluxe hướng biển',
  },
};

beforeEach(() => {
  vi.clearAllMocks();
  globalThis.ResizeObserver = class ResizeObserver {
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
  vi.mocked(useHousekeepingAction).mockReturnValue({
    mutate,
    isPending: false,
    variables: undefined,
  } as never);
});

afterEach(cleanup);

const queryResult = (overrides: Record<string, unknown> = {}) => ({
  data: undefined,
  error: null,
  isLoading: false,
  isFetching: false,
  isError: false,
  refetch,
  ...overrides,
});

describe('HousekeepingPage', () => {
  it('hiển thị danh sách phòng cần dọn và thông tin vận hành', () => {
    vi.mocked(useDirtyRooms).mockReturnValue(
      queryResult({
        data: {
          content: [dirtyRoom],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        },
      }) as never,
    );

    render(<HousekeepingPage />);

    expect(screen.getByRole('heading', { name: 'Phòng cần dọn' })).not.toBeNull();
    expect(screen.getByText('DLX-101')).not.toBeNull();
    expect(screen.getByText('Deluxe hướng biển')).not.toBeNull();
    expect(screen.getByText('DLX')).not.toBeNull();
    expect(screen.getByText('Tầng 1')).not.toBeNull();
    expect(screen.getByText('Khách yêu cầu kiểm tra minibar')).not.toBeNull();
    expect(screen.getByText('Chờ dọn')).not.toBeNull();
    expect(screen.getByRole('button', { name: 'Bắt đầu dọn' })).not.toBeNull();
    expect(useDirtyRooms).toHaveBeenCalledWith({ page: 0, size: 20 });
  });

  it('tính thống kê trang hiện tại từ dữ liệu API', () => {
    vi.mocked(useDirtyRooms).mockReturnValue(
      queryResult({
        data: {
          content: [
            dirtyRoom,
            { ...dirtyRoom, roomId: 'room-2', roomNumber: 'DLX-102', note: null },
            { ...dirtyRoom, roomId: 'room-3', roomNumber: 'STE-201', floor: 2 },
          ],
          page: 0,
          size: 20,
          totalElements: 3,
          totalPages: 1,
          first: true,
          last: true,
        },
      }) as never,
    );

    render(<HousekeepingPage />);

    expect(screen.getByText('Tổng phòng cần dọn')).not.toBeNull();
    expect(screen.getByText('Tầng trong trang hiện tại')).not.toBeNull();
    expect(screen.getByText('Phòng có ghi chú trong trang')).not.toBeNull();
    expect(screen.getByText('3 phòng đang chờ xử lý')).not.toBeNull();
  });

  it('gọi mutation complete khi housekeeper hoàn tất dọn phòng CLEANING', () => {
    vi.mocked(useDirtyRooms).mockReturnValue(
      queryResult({
        data: {
          content: [{ ...dirtyRoom, status: 'CLEANING' }],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        },
      }) as never,
    );

    render(<HousekeepingPage />);

    expect(screen.getByText('Đang dọn')).not.toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Hoàn tất dọn' }));

    expect(mutate).toHaveBeenCalledWith(
      { roomId: dirtyRoom.roomId, action: 'complete' },
      expect.objectContaining({ onSuccess: expect.any(Function), onError: expect.any(Function) }),
    );
  });

  it('không hiển thị thao tác hoàn tất cho phòng đã AVAILABLE ngoài dữ liệu stale', () => {
    vi.mocked(useDirtyRooms).mockReturnValue(
      queryResult({
        data: {
          content: [{ ...dirtyRoom, status: 'AVAILABLE' }],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        },
      }) as never,
    );

    render(<HousekeepingPage />);

    expect(screen.getByText('Sẵn sàng')).not.toBeNull();
    expect(screen.queryByRole('button', { name: 'Hoàn tất dọn' })).toBeNull();
  });

  it('khóa các thao tác khác trong khi một phòng đang được cập nhật', () => {
    vi.mocked(useHousekeepingAction).mockReturnValue({
      mutate,
      isPending: true,
      variables: { roomId: dirtyRoom.roomId, action: 'start' },
    } as never);
    vi.mocked(useDirtyRooms).mockReturnValue(
      queryResult({
        data: {
          content: [
            dirtyRoom,
            { ...dirtyRoom, roomId: 'room-2', roomNumber: 'DLX-102', status: 'CLEANING' },
          ],
          page: 0,
          size: 20,
          totalElements: 2,
          totalPages: 1,
          first: true,
          last: true,
        },
      }) as never,
    );

    render(<HousekeepingPage />);

    expect(
      screen.getByRole('button', { name: /Bắt đầu dọn/ }).classList.contains('ant-btn-loading'),
    ).toBe(true);
    expect(screen.getByRole('button', { name: 'Hoàn tất dọn' }).hasAttribute('disabled')).toBe(
      true,
    );
  });

  it('chuyển page UI một-based thành page API zero-based', () => {
    vi.mocked(useDirtyRooms).mockReturnValue(
      queryResult({
        data: {
          content: [dirtyRoom],
          page: 0,
          size: 20,
          totalElements: 21,
          totalPages: 2,
          first: true,
          last: false,
        },
      }) as never,
    );

    render(<HousekeepingPage />);
    const nextButton = document.querySelector<HTMLButtonElement>('.ant-pagination-next button');
    expect(nextButton).not.toBeNull();
    fireEvent.click(nextButton!);

    expect(useDirtyRooms).toHaveBeenLastCalledWith({ page: 1, size: 20 });
  });

  it('quay về trang cuối còn tồn tại khi dữ liệu đồng thời bị giảm', async () => {
    vi.mocked(useDirtyRooms).mockReturnValue(
      queryResult({
        data: {
          content: [dirtyRoom],
          page: 0,
          size: 20,
          totalElements: 21,
          totalPages: 2,
          first: true,
          last: false,
        },
      }) as never,
    );

    const { rerender } = render(<HousekeepingPage />);
    const nextButton = document.querySelector<HTMLButtonElement>('.ant-pagination-next button');
    fireEvent.click(nextButton!);

    vi.mocked(useDirtyRooms).mockReturnValue(
      queryResult({
        data: {
          content: [],
          page: 1,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          first: false,
          last: true,
        },
      }) as never,
    );
    rerender(<HousekeepingPage />);

    await waitFor(() => {
      expect(useDirtyRooms).toHaveBeenLastCalledWith({ page: 0, size: 20 });
    });
  });

  it('hiển thị ghi chú trống an toàn', () => {
    vi.mocked(useDirtyRooms).mockReturnValue(
      queryResult({
        data: {
          content: [{ ...dirtyRoom, note: null }],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        },
      }) as never,
    );

    render(<HousekeepingPage />);

    expect(screen.getByText('Không có ghi chú')).not.toBeNull();
  });

  it('hiển thị empty state và cho phép làm mới', () => {
    vi.mocked(useDirtyRooms).mockReturnValue(
      queryResult({
        data: {
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true,
        },
      }) as never,
    );

    render(<HousekeepingPage />);

    expect(screen.getByText('Hiện không có phòng nào cần dọn')).not.toBeNull();
    fireEvent.click(screen.getAllByText('Làm mới')[0]);
    expect(refetch).toHaveBeenCalledTimes(1);
  });

  it('hiển thị lỗi và retry request', () => {
    vi.mocked(useDirtyRooms).mockReturnValue(
      queryResult({ isError: true, error: new Error('Máy chủ không phản hồi') }) as never,
    );

    render(<HousekeepingPage />);

    expect(screen.getByText('Không thể tải danh sách phòng cần dọn')).not.toBeNull();
    expect(screen.getByText('Máy chủ không phản hồi')).not.toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Thử tải lại' }));
    expect(refetch).toHaveBeenCalledTimes(1);
  });

  it('hiển thị loading state mà không hiện empty state', () => {
    vi.mocked(useDirtyRooms).mockReturnValue(queryResult({ isLoading: true }) as never);

    render(<HousekeepingPage />);

    expect(screen.queryByText('Hiện không có phòng nào cần dọn')).toBeNull();
    expect(document.querySelector('.ant-table-wrapper')).not.toBeNull();
  });
});
