// @vitest-environment jsdom

import { beforeEach, describe, expect, it } from 'vitest';

import { authStorage, tokenStorage } from './storage';

describe('tokenStorage', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('stores access and refresh tokens together', () => {
    tokenStorage.setTokens('access-token', 'refresh-token');

    expect(tokenStorage.getAccessToken()).toBe('access-token');
    expect(tokenStorage.getRefreshToken()).toBe('refresh-token');
  });

  it('replaces both tokens after refresh', () => {
    tokenStorage.setTokens('old-access-token', 'old-refresh-token');
    tokenStorage.setTokens('new-access-token', 'new-refresh-token');

    expect(tokenStorage.getAccessToken()).toBe('new-access-token');
    expect(tokenStorage.getRefreshToken()).toBe('new-refresh-token');
  });

  it('clears tokens when the session expires', () => {
    authStorage.setSession('access-token', [], 3600, true, 'refresh-token');

    authStorage.clearSession();

    expect(tokenStorage.getAccessToken()).toBeNull();
    expect(tokenStorage.getRefreshToken()).toBeNull();
  });

  it('updates the stored authenticated user without replacing tokens or roles', () => {
    authStorage.setSession('access-token', [], 3600, true, 'refresh-token', {
      id: '96a8f221-c24d-4a52-a91d-b110fbde8c0e',
      email: 'guest@example.com',
      fullName: 'Nguyễn Văn A',
      phone: '0901234567',
    });

    authStorage.updateUser({
      id: '96a8f221-c24d-4a52-a91d-b110fbde8c0e',
      email: 'guest@example.com',
      fullName: 'Nguyễn Văn B',
      phone: '0987654321',
    });

    expect(authStorage.getSession()).toMatchObject({
      accessToken: 'access-token',
      roles: [],
      user: { fullName: 'Nguyễn Văn B', phone: '0987654321' },
    });
    expect(tokenStorage.getRefreshToken()).toBe('refresh-token');
  });
});

it('updates cached roles when a refreshed JWT contains new permissions', () => {
  authStorage.clearSession();
  authStorage.setSession('old-token', ['RECEPTIONIST'], 3600, true, 'old-refresh');
  const payload = btoa(JSON.stringify({ roles: ['HOUSEKEEPER', 'MANAGER'] }));
  authStorage.updateTokens(`header.${payload}.signature`, 'new-refresh', 3600);
  expect(authStorage.getSession()?.roles).toEqual(['HOUSEKEEPER', 'MANAGER']);
  authStorage.clearSession();
});
