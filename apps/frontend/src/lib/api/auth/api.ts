import { apiClient } from '../client';
import { setAccessToken } from '../token';
import type { ApiResponse } from '../types';
import type { SignUpRequest, AuthResponse } from './types';

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
