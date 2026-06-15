import axios, {
  AxiosHeaders,
  type AxiosError,
  type InternalAxiosRequestConfig,
  type AxiosRequestConfig,
} from 'axios';
import { authStore } from '../../store/auth.store';
import { refreshToken } from '@/features/auth/api';
import { handleApiError } from './error-handler';

const API_BASE_URL: string =
  import.meta.env.VITE_BASE_URL || 'http://localhost:4000/api/v1.0';

const PUBLIC_BASE_URL: string =
  import.meta.env.VITE_PUBLIC_BASE_URL ||
  'http://localhost:8080/public/api/v1.0';

export type ErrorPolicy = 'default' | 'suppress-toast' | 'bypass';

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

type RequestConfigMeta = {
  _retry?: boolean;
  _skipAuth?: boolean;
  errorPolicy?: ErrorPolicy;
};

export type RequestConfigWithMeta = AxiosRequestConfig & RequestConfigMeta;

type InternalRequestConfigWithMeta = InternalAxiosRequestConfig &
  RequestConfigMeta;

const setAuthorizationHeader = (
  config: InternalRequestConfigWithMeta,
  token: string,
) => {
  config.headers = AxiosHeaders.from(config.headers);
  config.headers.set('Authorization', `Bearer ${token}`);
};

apiClient.interceptors.request.use(
  async (config: InternalRequestConfigWithMeta) => {
    const currentToken = authStore.accessToken;
    if (currentToken) {
      setAuthorizationHeader(config, currentToken);
      return config;
    }

    const newToken = await refreshToken({ errorPolicy: 'suppress-toast' });
    if (newToken) {
      setAuthorizationHeader(config, newToken);
    }
    return config;
  },
);

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const responseStatus = error.response?.status;
    const config = error.config as InternalRequestConfigWithMeta | undefined;
    if (!config || config._retry) {
      return Promise.reject(error);
    }

    if (responseStatus === 401) {
      config._retry = true;
      const newToken = await refreshToken({ errorPolicy: 'suppress-toast' });
      if (newToken) {
        setAuthorizationHeader(config, newToken);
        return apiClient(config);
      }
    }

    return Promise.reject(error);
  },
);

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const config = error.config as InternalRequestConfigWithMeta | undefined;
    if (config?.errorPolicy === 'bypass') {
      return Promise.reject(error);
    }

    return handleApiError(error, {
      suppressToast: config?.errorPolicy === 'suppress-toast',
    });
  },
);

publicClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const config = error.config as RequestConfigWithMeta | undefined;
    if (config?.errorPolicy === 'bypass') {
      return Promise.reject(error);
    }

    return handleApiError(error, {
      suppressToast: config?.errorPolicy === 'suppress-toast',
    });
  },
);
