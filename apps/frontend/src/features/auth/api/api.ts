import type { AxiosResponse } from 'axios';
import {
  apiClient,
  publicClient,
  type ErrorPolicy,
  type RequestConfigWithMeta,
} from '@/lib/api/client';
import type {
  AuthResponse,
  SendSignUpEmailCodeRequest,
  SignInRequest,
  SignUpEmailVerificationResponse,
  SignUpRequest,
  VerifySignUpEmailRequest,
  VerifySignUpEmailResponse,
} from './types';

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

export const sendSignUpEmailCode = async (
  data: SendSignUpEmailCodeRequest,
): Promise<SignUpEmailVerificationResponse> => {
  const response = await publicClient.post<SignUpEmailVerificationResponse>(
    '/users/signup/email-code',
    data,
  );

  return response.data;
};

export const verifySignUpEmail = async (
  data: VerifySignUpEmailRequest,
): Promise<VerifySignUpEmailResponse> => {
  const response = await publicClient.post<VerifySignUpEmailResponse>(
    '/users/signup/email-code/verify',
    data,
  );

  return response.data;
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
