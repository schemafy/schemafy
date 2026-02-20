import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { authStore } from '../../store/auth.store';
import { refreshToken } from '@/features/auth/api';

const BFF_API_BASE_URL: string =
  import.meta.env.VITE_BFF_URL || 'http://localhost:4000/api/v1.0';

export const bffClient = axios.create({
  baseURL: BFF_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
  paramsSerializer: (params) => {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (Array.isArray(value)) {
        value.forEach((item) => searchParams.append(key, item));
      } else if (value !== undefined && value !== null) {
        searchParams.append(key, String(value));
      }
    });
    return searchParams.toString();
  },
});

type RequestConfigWithMeta = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

bffClient.interceptors.request.use(async (config: RequestConfigWithMeta) => {
  const currentToken = authStore.accessToken;
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
      const newToken = await refreshToken();
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
