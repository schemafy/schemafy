import path from 'path';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vite.dev/config/
export default defineConfig(({ command }) => {
  const isDev = command === 'serve';
  const aliases: Record<string, string> = {
    '@': path.resolve(__dirname, './src'),
  };

  // Only add validator alias in development for faster build times
  if (isDev) {
    aliases['@schemafy/validator'] = path.resolve(__dirname, '../../packages/validator/src');
  }

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: aliases,
    },
    server: isDev
      ? {
        port: 3001,
        fs: {
          allow: [path.resolve(__dirname), path.resolve(__dirname, '../../packages/validator/src')],
        },
      }
      : undefined,
  };
});
