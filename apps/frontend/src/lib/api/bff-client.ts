import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { AuthStore } from '../../store/auth.store';

const BFF_URL: string =
  (import.meta as unknown as { env?: { VITE_BFF_URL?: string } })?.env
    ?.VITE_BFF_URL ?? 'http://localhost:4000';

export const bffClient = axios.create({
  baseURL: BFF_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

const refreshClient = axios.create({
  baseURL: BFF_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

let refreshPromise: Promise<string | null> | null = null;

const refreshAccessToken = async (): Promise<string | null> => {
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

type RequestConfigWithMeta = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

bffClient.interceptors.request.use(async (config: RequestConfigWithMeta) => {
  const currentToken = AuthStore.getInstance().accessToken;
  if (currentToken) {
    config.headers = config.headers ?? {};
    (config.headers as Record<string, string>)['Authorization'] =
      `Bearer ${currentToken}`;
    return config;
  }

  const newToken = await refreshAccessToken();
  if (newToken) {
    config.headers = config.headers ?? {};
    (config.headers as Record<string, string>)['Authorization'] =
      `Bearer ${newToken}`;
  }
  return config;
});

bffClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const responseStatus = error.response?.status;
    const config = error.config as RequestConfigWithMeta | undefined;
    if (!config || config._retry) {
      return Promise.reject(error);
    }

    if (responseStatus === 401) {
      config._retry = true;
      const newToken = await refreshAccessToken();
      if (newToken) {
        config.headers = config.headers ?? {};
        (config.headers as Record<string, string>)['Authorization'] =
          `Bearer ${newToken}`;
        return bffClient(config);
      }
    }

    return Promise.reject(error);
  },
);
