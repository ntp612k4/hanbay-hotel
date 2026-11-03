import type { ReactNode } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { App as AntdApp, ConfigProvider } from 'antd';
import viVN from 'antd/locale/vi_VN';
import 'dayjs/locale/vi';

import { aegeanTheme } from '@config/theme';
import { AuthProvider } from '@features/authentication/providers/auth-provider';

import { queryClient } from './query-client';

interface AppProvidersProps {
  children: ReactNode;
}

export function AppProviders({ children }: AppProvidersProps) {
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={viVN} theme={aegeanTheme}>
        <AntdApp>
          <AuthProvider>{children}</AuthProvider>
        </AntdApp>
      </ConfigProvider>
    </QueryClientProvider>
  );
}
