import { apiClient } from '../client';
import { setAccessToken, getAccessToken } from '../token';
import type { ApiResponse } from '../types';
import type { SignInRequest, SignUpRequest, AuthResponse } from './types';

export const signUp = async (
  data: SignUpRequest,
): Promise<ApiResponse<AuthResponse>> => {
  const response = await apiClient.post<ApiResponse<AuthResponse>>(
    '/api/v1.0/users/signup',
    data,
  );

  const accessToken = response.headers['authorization'];
  if (accessToken && accessToken.startsWith('Bearer ')) {
    const token = accessToken.replace('Bearer ', '');
    setAccessToken(token);
  }

  return response.data;
};

export const signIn = async (
  data: SignInRequest,
): Promise<ApiResponse<AuthResponse>> => {
  const response = await apiClient.post<ApiResponse<AuthResponse>>(
    '/api/v1.0/users/login',
    data,
  );

  const accessToken = response.headers['authorization'];
  if (accessToken && accessToken.startsWith('Bearer ')) {
    const token = accessToken.replace('Bearer ', '');
    setAccessToken(token);
  }

  return response.data;
};

export const refreshToken = async (): Promise<ApiResponse<null>> => {
  const response = await apiClient.post<ApiResponse<null>>(
    '/api/v1.0/users/refresh',
  );

  const accessToken = response.headers['authorization'];
  if (accessToken && accessToken.startsWith('Bearer ')) {
    const token = accessToken.replace('Bearer ', '');
    setAccessToken(token);
  }

  return response.data;
};

export const getUserInfo = async (
  userId: string,
): Promise<ApiResponse<AuthResponse>> => {
  const token = getAccessToken();

  const response = await apiClient.get<ApiResponse<AuthResponse>>(
    `/api/v1.0/users/${userId}`,
    {
      headers: {
        Authorization: token ? `Bearer ${token}` : '',
      },
    },
  );

  return response.data;
};
