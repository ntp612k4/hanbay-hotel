import { z } from 'zod';

import { publicApiClient } from '../../../lib/axios';
import { userRoleSchema, type UserRole } from '../../../types/common';

interface LoginRequest {
  identifier: string;
  password: string;
}

const loginEnvelopeSchema = z.object({
  success: z.literal(true),
  message: z.string(),
  data: z.object({
    accessToken: z.string().min(1),
    refreshToken: z.string().min(1),
    tokenType: z.literal('Bearer'),
    expiresIn: z.number().positive(),
    user: z.object({
      id: z.uuid(),
      fullName: z.string().min(1),
      email: z.email(),
      phone: z.string().nullable(),
      roles: z.array(userRoleSchema),
    }),
  }),
});

type LoginUser = z.infer<typeof loginEnvelopeSchema>['data']['user'];

export async function login(request: LoginRequest): Promise<{
  accessToken: string;
  refreshToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  userRoles: UserRole[];
  user: LoginUser;
}> {
  const response = await publicApiClient.post('/auth/login', request);
  const envelope = loginEnvelopeSchema.parse(response.data);
  const payload = envelope.data;

  return {
    accessToken: payload.accessToken,
    refreshToken: payload.refreshToken,
    tokenType: payload.tokenType,
    expiresIn: payload.expiresIn,
    userRoles: payload.user.roles,
    user: payload.user,
  };
}
