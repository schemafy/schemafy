import type { AxiosResponse } from 'axios';
import {
  apiClient,
  publicClient,
  type ErrorPolicy,
  type RequestConfigWithMeta,
} from '@/lib/api/client';
import type { SignInRequest, SignUpRequest, AuthResponse } from './types';

import { authStore } from '@/store/auth.store';
import { clearAuthSession } from '../lib/auth-session';

let refreshPromise: Promise<string> | null = null;

const handleTokenResponse = (response: AxiosResponse): string => {
  const accessToken = response.headers['authorization'];
  if (accessToken && accessToken.startsWith('Bearer ')) {
    const token = accessToken.replace('Bearer ', '');
    authStore.setAccessToken(token);
    return token;
  } else {
    throw new Error('Failed to get token');
  }
};

export const signUp = async (data: SignUpRequest): Promise<AuthResponse> => {
  const response = await publicClient.post<AuthResponse>('/users/signup', data);

  handleTokenResponse(response);

  return response.data;
};

export const signIn = async (data: SignInRequest): Promise<AuthResponse> => {
  const response = await publicClient.post<AuthResponse>('/users/login', data);

  handleTokenResponse(response);

  return response.data;
};

export const refreshToken = async (
  options: { errorPolicy?: ErrorPolicy } = {},
): Promise<string> => {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const config: RequestConfigWithMeta | undefined = options.errorPolicy
          ? { errorPolicy: options.errorPolicy }
          : undefined;
        const response = await publicClient.post(
          '/users/refresh',
          undefined,
          config,
        );
        return handleTokenResponse(response);
      } catch (error) {
        clearAuthSession();
        throw error;
      } finally {
        refreshPromise = null;
      }
    })();
  }
  return refreshPromise;
};

export const getMyInfo = async (): Promise<AuthResponse> => {
  const response = await apiClient.get<AuthResponse>(`/users`);

  return response.data;
};

export const logout = async (): Promise<void> => {
  await apiClient.post('/users/logout');
};
