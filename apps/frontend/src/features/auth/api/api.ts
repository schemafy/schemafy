import type { AxiosResponse } from 'axios';
import { apiClient, publicClient } from '@/lib/api/client';
import type { SignInRequest, SignUpRequest, AuthResponse } from './types';

import { authStore } from '@/store/auth.store';

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

export const refreshToken = async (): Promise<string> => {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const response = await publicClient.post('/users/refresh');
        return handleTokenResponse(response);
      } catch (error) {
        authStore.clearAuth();
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
