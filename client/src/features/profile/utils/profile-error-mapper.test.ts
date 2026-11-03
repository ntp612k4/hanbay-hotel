import { describe, expect, it } from 'vitest';

import { ApiClientError } from '../../../types/api';
import { getProfileErrorMessage, getProfileFieldErrors } from './profile-error-mapper';

describe('profileErrorMapper', () => {
  it('maps backend field errors to editable profile fields', () => {
    const error = new ApiClientError({
      message: 'Dữ liệu không hợp lệ',
      kind: 'api',
      status: 400,
      data: {
        errors: { phone: 'Số điện thoại không hợp lệ', email: 'Không được hỗ trợ' },
      },
    });

    expect(getProfileFieldErrors(error)).toEqual({ phone: 'Số điện thoại không hợp lệ' });
  });

  it('returns a friendly duplicate phone message', () => {
    const error = new ApiClientError({
      message: 'Số điện thoại đã tồn tại: 0987654321',
      kind: 'api',
      status: 409,
      code: 'PHONE_ALREADY_EXISTS',
    });

    expect(getProfileErrorMessage(error)).toBe('Số điện thoại này đã được sử dụng.');
  });
});
