import { z } from 'zod';

import { publicApiClient } from '../../../../lib/axios';
import type {
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  ResetPasswordRequest,
  ResetPasswordResponse,
  ResetTokenVerificationResponse,
} from '../types/password-recovery.types';

const forgotPasswordEnvelopeSchema = z.object({
  success: z.literal(true),
  message: z.string(),
  data: z.object({ status: z.literal('RESET_INSTRUCTIONS_ACCEPTED') }),
});

const resetTokenEnvelopeSchema = z.object({
  success: z.literal(true),
  message: z.string(),
  data: z.object({
    status: z.literal('RESET_TOKEN_VALID'),
    expiresAt: z.iso.datetime(),
  }),
});

const resetPasswordEnvelopeSchema = z.object({
  success: z.literal(true),
  message: z.string(),
  data: z.object({ status: z.literal('PASSWORD_RESET') }),
});

export async function forgotPassword(
  request: ForgotPasswordRequest,
): Promise<ForgotPasswordResponse> {
  const response = await publicApiClient.post('/auth/forgot-password', request);
  return forgotPasswordEnvelopeSchema.parse(response.data).data;
}

export async function verifyResetToken(token: string): Promise<ResetTokenVerificationResponse> {
  const response = await publicApiClient.post('/auth/reset-password/verify', { token });
  return resetTokenEnvelopeSchema.parse(response.data).data;
}

export async function resetPassword(request: ResetPasswordRequest): Promise<ResetPasswordResponse> {
  const response = await publicApiClient.post('/auth/reset-password', request);
  return resetPasswordEnvelopeSchema.parse(response.data).data;
}
