import path from 'path';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// https://vite.dev/config/
export default defineConfig(({ command, mode }) => {
  const isDev = command === 'serve';
  const rootEnv = loadEnv(
    mode,
    path.resolve(__dirname, '../..'),
    'SMTP_ENABLED',
  );
  const defaultSmtpEnabled = isDev ? 'false' : 'true';
  const smtpEnabled =
    process.env.SMTP_ENABLED ?? rootEnv.SMTP_ENABLED ?? defaultSmtpEnabled;
  const aliases: Record<string, string> = {
    '@': path.resolve(__dirname, './src'),
  };

  return {
    define: {
      'import.meta.env.SMTP_ENABLED': JSON.stringify(smtpEnabled),
    },
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: aliases,
      dedupe: ['react', 'react-dom', 'react-router-dom'],
    },
    server: isDev
      ? {
          port: 3001,
        }
      : undefined,
  };
});
