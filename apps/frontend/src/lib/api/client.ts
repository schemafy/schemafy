import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { AuthStore } from '../../store/auth.store';

const BASE_URL: string =
  import.meta.env.VITE_BASE_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // 쿠키 전송을 위해 필요 (refresh token)
});

// 인증이 필요 없는 요청은 별도의 publicClient를 사용하도록 유도한다.
export const publicClient = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

type RequestConfigWithMeta = InternalAxiosRequestConfig & {
  _retry?: boolean;
  _skipAuth?: boolean;
};

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

apiClient.interceptors.request.use(async (config: RequestConfigWithMeta) => {
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

apiClient.interceptors.response.use(
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
        return apiClient(config);
      }
    }

    return Promise.reject(error);
  },
);
