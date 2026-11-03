import { ApiClientError } from '../../../../types/api';
import type { GuestRegisterFormValues } from '../schemas/guestRegisterSchema';

const ERROR_MESSAGES: Record<string, string> = {
  EMAIL_ALREADY_EXISTS: 'Email này đã được đăng ký.',
  PHONE_ALREADY_EXISTS: 'Phone number is already registered.',
  PASSWORD_MISMATCH: 'Password and confirm password do not match.',
  VALIDATION_ERROR: 'Please check the registration information.',
};

const GUEST_REGISTER_FIELDS = [
  'fullName',
  'email',
  'phone',
  'password',
  'confirmPassword',
] as const satisfies readonly (keyof GuestRegisterFormValues)[];

type GuestRegisterFieldName = (typeof GUEST_REGISTER_FIELDS)[number];

const GUEST_REGISTER_FIELD_SET = new Set<string>(GUEST_REGISTER_FIELDS);

function normalizeFieldError(error: string | string[]): string {
  return Array.isArray(error) ? error[0] : error;
}

export function mapGuestRegisterError(error: Error): string {
  if (error instanceof ApiClientError && error.code) {
    return ERROR_MESSAGES[error.code] ?? error.message;
  }

  return error.message;
}

export function getGuestRegisterFieldErrors(
  error: Error,
): Partial<Record<GuestRegisterFieldName, string>> {
  if (!(error instanceof ApiClientError) || !error.data?.errors) {
    return {};
  }

  return Object.entries(error.data.errors).reduce<Partial<Record<GuestRegisterFieldName, string>>>(
    (fieldErrors, [fieldName, fieldError]) => {
      if (GUEST_REGISTER_FIELD_SET.has(fieldName)) {
        fieldErrors[fieldName as GuestRegisterFieldName] = normalizeFieldError(fieldError);
      }

      return fieldErrors;
    },
    {},
  );
}
