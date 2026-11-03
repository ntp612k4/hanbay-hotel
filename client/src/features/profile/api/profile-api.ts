import { apiClient } from '../../../lib/axios';
import { userProfileEnvelopeSchema } from '../schemas/profile-schema';
import type { UpdateUserRequest, User } from '../types/profile';

export async function getCurrentProfile(): Promise<User> {
  const response = await apiClient.get('/users/me');
  return userProfileEnvelopeSchema.parse(response.data).data;
}

export async function updateCurrentProfile(request: UpdateUserRequest): Promise<User> {
  const response = await apiClient.patch('/users/me', request);
  return userProfileEnvelopeSchema.parse(response.data).data;
}
