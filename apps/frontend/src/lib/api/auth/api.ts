import type { AxiosResponse } from 'axios';
import { apiClient, publicClient } from '../client';
import { setAccessToken } from '../token';
import type { ApiResponse } from '../types';
import type { SignInRequest, SignUpRequest, AuthResponse } from './types';

import { AuthStore } from '@/store';

let refreshPromise: Promise<string> | null = null;

const handleTokenResponse = (
  response: AxiosResponse<ApiResponse<unknown>>,
): string => {
  const accessToken = response.headers['authorization'];
  if (accessToken && accessToken.startsWith('Bearer ')) {
    const token = accessToken.replace('Bearer ', '');
    setAccessToken(token);
    return token;
  } else {
    throw new Error('Failed to get token');
  }
};

export const signUp = async (
  data: SignUpRequest,
): Promise<ApiResponse<AuthResponse>> => {
  const response = await publicClient.post<ApiResponse<AuthResponse>>(
    '/public/api/v1.0/users/signup',
    data,
  );

  handleTokenResponse(response);

  return response.data;
};

export const signIn = async (
  data: SignInRequest,
): Promise<ApiResponse<AuthResponse>> => {
  const response = await publicClient.post<ApiResponse<AuthResponse>>(
    '/public/api/v1.0/users/login',
    data,
  );

  handleTokenResponse(response);

  return response.data;
};

export const refreshToken = async (): Promise<string> => {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const response = await publicClient.post<ApiResponse<null>>(
          '/public/api/v1.0/users/refresh',
        );
        return handleTokenResponse(response);
      } catch (error) {
        AuthStore.getInstance().clearAuth();
        throw error;
      } finally {
        refreshPromise = null;
      }
    })();
  }
  return refreshPromise;
};

export const getMyInfo = async (): Promise<ApiResponse<AuthResponse>> => {
  const response =
    await apiClient.get<ApiResponse<AuthResponse>>(`/api/v1.0/users`);

  if (!response.data.success) {
    throw new Error('Failed to get my info');
  }

  return response.data;
};
