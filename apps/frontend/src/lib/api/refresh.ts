import { AuthStore } from '@/store';
import axios from 'axios';

const BASE_URL: string =
  import.meta.env.VITE_BASE_URL || 'http://localhost:8080';

const refreshClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

let refreshPromise: Promise<string | null> | null = null;

export const refreshAccessToken = async (): Promise<string | null> => {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const response = await refreshClient.post(
          '/public/api/v1.0/users/refresh',
        );
        const authHeader: string | undefined =
          response.headers['authorization'];
        if (authHeader && authHeader.startsWith('Bearer ')) {
          const token = authHeader.replace('Bearer ', '');
          AuthStore.getInstance().setAccessToken(token);
          return token;
        }
        AuthStore.getInstance().clearAuth();
        throw new Error('REFRESH_NO_AUTH_HEADER');
      } catch (e) {
        AuthStore.getInstance().clearAuth();
        throw e instanceof Error ? e : new Error('REFRESH_FAILED');
      } finally {
        refreshPromise = null;
      }
    })();
  }
  return refreshPromise;
};
