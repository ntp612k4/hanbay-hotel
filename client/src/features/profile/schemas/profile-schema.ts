import { z } from 'zod';

import { userRoleSchema } from '../../../types/common';

export const userProfileSchema = z.object({
  id: z.uuid(),
  email: z.email(),
  fullName: z.string(),
  phone: z.string().nullable(),
  status: z.enum(['ACTIVE', 'INACTIVE', 'LOCKED']),
  roles: z.array(userRoleSchema),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export const userProfileEnvelopeSchema = z.object({
  success: z.literal(true),
  message: z.string(),
  data: userProfileSchema,
});
