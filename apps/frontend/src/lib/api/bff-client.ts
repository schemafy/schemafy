import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { AuthStore } from '../../store/auth.store';
import { refreshAccessToken } from './refresh';

const BFF_URL: string = import.meta.env.VITE_BFF_URL || 'http://localhost:4000';

export const bffClient = axios.create({
  baseURL: BFF_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

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
