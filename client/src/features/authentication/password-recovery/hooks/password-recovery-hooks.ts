import { useMutation, useQuery } from '@tanstack/react-query';

import { forgotPassword, resetPassword, verifyResetToken } from '../api/password-recovery-api';

export function useForgotPassword() {
  return useMutation({ mutationFn: forgotPassword });
}

export function useVerifyResetToken(token: string | null) {
  return useQuery({
    queryKey: ['authentication', 'reset-password', 'verify', token],
    queryFn: () => verifyResetToken(token ?? ''),
    enabled: Boolean(token),
    retry: false,
  });
}

export function useResetPassword() {
  return useMutation({ mutationFn: resetPassword });
}
