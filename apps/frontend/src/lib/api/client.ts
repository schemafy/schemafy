import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { authStore } from '../../store/auth.store';
import { refreshToken } from '@/features/auth/api';
import { handleApiError } from './error-handler';
import { operationHistoryStore } from '@/store/operation-history.store';

const API_BASE_URL: string =
  import.meta.env.VITE_BASE_URL || 'http://localhost:4000/api/v1.0';

const PUBLIC_BASE_URL: string =
  import.meta.env.VITE_PUBLIC_BASE_URL ||
  'http://localhost:8080/public/api/v1.0';

const commonConfig = {
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
  paramsSerializer: (params: Record<string, unknown>) => {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (Array.isArray(value)) {
        value.forEach((item) => searchParams.append(key, String(item)));
      } else if (value !== undefined && value !== null) {
        searchParams.append(key, String(value));
      }
    });
    return searchParams.toString();
  },
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

const getHeaderValue = (
  headers: InternalAxiosRequestConfig['headers'] | undefined,
  name: string,
): string | null => {
  if (!headers) return null;

  const value =
    typeof headers.get === 'function'
      ? headers.get(name)
      : (headers as Record<string, string | undefined>)[name];

  if (typeof value === 'string') return value;
  if (typeof value === 'number') return String(value);
  if (Array.isArray(value)) {
    const firstValue = value[0];
    return typeof firstValue === 'string' ? firstValue : null;
  }
  return null;
};

apiClient.interceptors.request.use(async (config: RequestConfigWithMeta) => {
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

apiClient.interceptors.response.use((response) => response, (error) => {
  const axiosError = error as AxiosError;
  const clientOperationId = getHeaderValue(
    axiosError.config?.headers,
    'X-Client-Op-Id',
  );

  if (clientOperationId) {
    operationHistoryStore.markFailed(clientOperationId, error);
  }

  return handleApiError(axiosError);
});
