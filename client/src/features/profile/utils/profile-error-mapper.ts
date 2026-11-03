import { ApiClientError } from '../../../types/api';

export type ProfileFieldName = 'fullName' | 'phone';

export function getProfileFieldErrors(error: Error): Partial<Record<ProfileFieldName, string>> {
  if (!(error instanceof ApiClientError) || !error.data?.errors) return {};

  const result: Partial<Record<ProfileFieldName, string>> = {};
  for (const field of ['fullName', 'phone'] as const) {
    const value = error.data.errors[field];
    if (value) result[field] = Array.isArray(value) ? value[0] : value;
  }
  return result;
}

export function getProfileErrorMessage(error: Error): string {
  if (error instanceof ApiClientError) {
    if (error.code === 'PHONE_ALREADY_EXISTS') return 'Số điện thoại này đã được sử dụng.';
    return error.message;
  }
  return 'Không thể hoàn thành yêu cầu. Vui lòng thử lại.';
}
