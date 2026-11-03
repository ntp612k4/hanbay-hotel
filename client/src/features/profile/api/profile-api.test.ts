import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '../../../lib/axios';
import { getCurrentProfile, updateCurrentProfile } from './profile-api';

vi.mock('../../../lib/axios', () => ({
  apiClient: {
    get: vi.fn(),
    patch: vi.fn(),
  },
}));

const profile = {
  id: '96a8f221-c24d-4a52-a91d-b110fbde8c0e',
  email: 'guest@example.com',
  fullName: 'Nguyễn Văn A',
  phone: '0901234567',
  status: 'ACTIVE',
  roles: [],
  createdAt: '2026-08-01T08:30:00Z',
  updatedAt: '2026-08-10T10:00:00Z',
};

const successEnvelope = { success: true, message: 'Thành công', data: profile };

describe('profileApi', () => {
  beforeEach(() => vi.clearAllMocks());

  it('loads the current profile using the authenticated endpoint', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: successEnvelope });

    await expect(getCurrentProfile()).resolves.toEqual(profile);
    expect(apiClient.get).toHaveBeenCalledWith('/users/me');
  });

  it('sends only supplied editable fields when updating profile', async () => {
    vi.mocked(apiClient.patch).mockResolvedValue({ data: successEnvelope });

    await updateCurrentProfile({ phone: '0987654321' });

    expect(apiClient.patch).toHaveBeenCalledWith('/users/me', { phone: '0987654321' });
  });

  it('rejects a response containing an unsupported role', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { ...successEnvelope, data: { ...profile, roles: ['UNKNOWN'] } },
    });

    await expect(getCurrentProfile()).rejects.toThrow();
  });
});
