import type { AxiosResponse } from 'axios';
import { apiClient, publicClient } from '@/lib/api/client';
import type { ApiResponse } from '@/lib/api';
import type { SignInRequest, SignUpRequest, AuthResponse } from './types';

import { authStore } from '@/store/auth.store';

let refreshPromise: Promise<string> | null = null;

const handleTokenResponse = (
  response: AxiosResponse<ApiResponse<unknown>>,
): string => {
  const accessToken = response.headers['authorization'];
  if (accessToken && accessToken.startsWith('Bearer ')) {
    const token = accessToken.replace('Bearer ', '');
    authStore.setAccessToken(token);
    return token;
  } else {
    throw new Error('Failed to get token');
  }
};

export const signUp = async (
  data: SignUpRequest,
): Promise<ApiResponse<AuthResponse>> => {
  const response = await publicClient.post<ApiResponse<AuthResponse>>(
    '/users/signup',
    data,
  );

  handleTokenResponse(response);

  return response.data;
};

export const signIn = async (
  data: SignInRequest,
): Promise<ApiResponse<AuthResponse>> => {
  const response = await publicClient.post<ApiResponse<AuthResponse>>(
    '/users/login',
    data,
  );

  handleTokenResponse(response);

  return response.data;
};

export const refreshToken = async (): Promise<string> => {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const response =
          await publicClient.post<ApiResponse<null>>('/users/refresh');
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

export const getMyInfo = async (): Promise<ApiResponse<AuthResponse>> => {
  const response = await apiClient.get<ApiResponse<AuthResponse>>(`/users`);

  if (!response.data.success) {
    throw new Error('Failed to get my info');
  }

  return response.data;
};

export const logout = async (): Promise<ApiResponse<null>> => {
  const response = await apiClient.post<ApiResponse<null>>('/users/logout');

  return response.data;
};
