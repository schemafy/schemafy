import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { AuthStore } from '../../store/auth.store';
import { refreshToken } from './auth/api';

const API_BASE_URL: string =
  import.meta.env.VITE_BASE_URL || 'http://localhost:8080/api/v1.0';

const PUBLIC_BASE_URL: string =
  import.meta.env.VITE_PUBLIC_BASE_URL ||
  'http://localhost:8080/public/api/v1.0';

const commonConfig = {
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
};

export const apiClient = axios.create({
  ...commonConfig,
  baseURL: API_BASE_URL,
});

export const publicClient = axios.create({
  ...commonConfig,
  baseURL: PUBLIC_BASE_URL,
});

type RequestConfigWithMeta = InternalAxiosRequestConfig & {
  _retry?: boolean;
  _skipAuth?: boolean;
};

apiClient.interceptors.request.use(async (config: RequestConfigWithMeta) => {
  const currentToken = AuthStore.getInstance().accessToken;
  if (currentToken) {
    config.headers = config.headers ?? {};
    (config.headers as Record<string, string>)['Authorization'] =
      `Bearer ${currentToken}`;
    return config;
  }

  const newToken = await refreshToken();
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
      const newToken = await refreshToken();
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
