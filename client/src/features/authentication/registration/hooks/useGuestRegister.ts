import { useMutation } from '@tanstack/react-query';

import { registerGuest } from '../api/guestRegisterApi';
import type { GuestRegisterRequest, GuestRegisterResponse } from '../types/guestRegister.types';

export function useGuestRegister() {
  return useMutation<GuestRegisterResponse, Error, GuestRegisterRequest>({
    mutationFn: registerGuest,
  });
}
