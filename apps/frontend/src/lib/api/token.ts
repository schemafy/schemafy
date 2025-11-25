import { useAuthStore } from '../../store/auth.store';

export const setAccessToken = (token: string): void => {
  useAuthStore.getState().setAccessToken(token);
};

export const getAccessToken = (): string | null => {
  return useAuthStore.getState().accessToken;
};

export const removeAccessToken = (): void => {
  useAuthStore.getState().clearAccessToken();
};

export const clearAuth = (): void => {
  removeAccessToken();
};
