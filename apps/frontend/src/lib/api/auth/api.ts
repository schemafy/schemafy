import type { AxiosResponse } from 'axios';
import { apiClient, publicClient } from '../client';
import { setAccessToken } from '../token';
import type { ApiResponse } from '../types';
import type { SignInRequest, SignUpRequest, AuthResponse } from './types';

const handleTokenResponse = (response: AxiosResponse<ApiResponse<unknown>>) => {
  const accessToken = response.headers['authorization'];
  if (accessToken && accessToken.startsWith('Bearer ')) {
    const token = accessToken.replace('Bearer ', '');
    setAccessToken(token);
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

export const refreshToken = async (): Promise<ApiResponse<null>> => {
  const response = await apiClient.post<ApiResponse<null>>(
    '/public/api/v1.0/users/refresh',
  );

  handleTokenResponse(response);

  return response.data;
};

export const getMyInfo = async (): Promise<ApiResponse<AuthResponse>> => {
  const response = await apiClient.get<ApiResponse<AuthResponse>>(
    `/public/api/v1.0/users`,
  );

  if (!response.data.success) {
    throw new Error('Failed to get my info');
  }

  return response.data;
};
