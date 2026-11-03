// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { useAuth } from '../features/authentication/providers/auth-context';
import { GuestRoute } from './guest-route';

vi.mock('../features/authentication/providers/auth-context', () => ({ useAuth: vi.fn() }));
afterEach(cleanup);

function renderRoute(roles: string[]) {
  vi.mocked(useAuth).mockReturnValue({ roles } as never);
  return render(
    <MemoryRouter initialEntries={['/account/bookings']}>
      <Routes>
        <Route path="/forbidden" element={<div>Không có quyền</div>} />
        <Route
          path="/account/bookings"
          element={
            <GuestRoute>
              <div>Lịch sử đặt phòng</div>
            </GuestRoute>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('GuestRoute', () => {
  it('allows an authenticated account without staff roles', () => {
    renderRoute([]);
    expect(screen.getByText('Lịch sử đặt phòng')).not.toBeNull();
  });

  it('rejects staff accounts', () => {
    renderRoute(['RECEPTIONIST']);
    expect(screen.getByText('Không có quyền')).not.toBeNull();
  });
});
