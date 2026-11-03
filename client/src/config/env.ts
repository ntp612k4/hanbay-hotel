const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;

if (!apiBaseUrl) {
  throw new Error('VITE_API_BASE_URL is required');
}

export const env = {
  apiBaseUrl,
  appName: import.meta.env.VITE_APP_NAME ?? 'Quản lý khách sạn',
} as const;
