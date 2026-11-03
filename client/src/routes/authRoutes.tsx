import { GuestRegisterPage } from '@features/authentication/registration';

import { ROUTE_PATHS } from './routePaths';

export const authRoutes = [
  {
    path: ROUTE_PATHS.guestRegister,
    element: <GuestRegisterPage />,
  },
  {
    path: ROUTE_PATHS.forgotPassword,
    lazy: async () => {
      const { ForgotPasswordPage } = await import('@features/authentication/password-recovery');
      return { Component: ForgotPasswordPage };
    },
  },
  {
    path: ROUTE_PATHS.resetPassword,
    lazy: async () => {
      const { ResetPasswordPage } = await import('@features/authentication/password-recovery');
      return { Component: ResetPasswordPage };
    },
  },
];
