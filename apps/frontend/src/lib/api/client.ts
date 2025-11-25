import axios, {
  type AxiosInstance,
  type AxiosError,
  type InternalAxiosRequestConfig,
} from 'axios';
import { useAuthStore } from '../../store/auth.store';

export const apiClient: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // 쿠키 전송을 위해 필요 (refresh token)
});

const isAuthFreePath = (url?: string | null) => {
  console.log(url);
  if (!url) return false;
  return (
    url.includes('/public/api/v1.0/users/login') ||
    url.includes('/public/api/v1.0/users/signup')
  );
};

type RequestConfigWithMeta = InternalAxiosRequestConfig & {
  _retry?: boolean;
  _skipAuth?: boolean;
};

const refreshClient = axios.create({
  baseURL: 'http://localhost:8080',
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
        const response = await refreshClient.post('/api/v1.0/users/refresh');
        const authHeader: string | undefined =
          response.headers['authorization'];
        if (authHeader && authHeader.startsWith('Bearer ')) {
          const token = authHeader.replace('Bearer ', '');
          useAuthStore.getState().setAccessToken(token);
          return token;
        }
        useAuthStore.getState().setAccessToken(null);
        return null;
      } catch {
        useAuthStore.getState().setAccessToken(null);
        return null;
      } finally {
        refreshPromise = null;
      }
    })();
  }
  return refreshPromise;
};

apiClient.interceptors.request.use(async (config: RequestConfigWithMeta) => {
  if (config._skipAuth) return config;

  const url = config.url ?? '';
  if (isAuthFreePath(url)) {
    return config;
  }

  const currentToken = useAuthStore.getState().accessToken;
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
    const url = config?.url ?? '';

    if (!config || config._retry || isAuthFreePath(url)) {
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
