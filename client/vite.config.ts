import { fileURLToPath, URL } from 'node:url';

import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    setupFiles: ['./src/test/setup.ts'],
    maxWorkers: 1,
  },
  server: {
    proxy: {
      '/api': {
            target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/actuator': {
            target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@components': fileURLToPath(new URL('./src/components', import.meta.url)),
      '@features': fileURLToPath(new URL('./src/features', import.meta.url)),
      '@services': fileURLToPath(new URL('./src/services', import.meta.url)),
      '@hooks': fileURLToPath(new URL('./src/hooks', import.meta.url)),
      '@utils': fileURLToPath(new URL('./src/utils', import.meta.url)),
      '@types': fileURLToPath(new URL('./src/types', import.meta.url)),
      '@config': fileURLToPath(new URL('./src/config', import.meta.url)),
    },
  },
});
