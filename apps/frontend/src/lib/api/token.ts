import { authStore } from '../../store/auth.store';

export const setAccessToken = (token: string | null) => {
  authStore.setAccessToken(token);
};

export const getAccessToken = () => {
  return authStore.accessToken;
};

export const removeAccessToken = () => {
  authStore.clearAccessToken();
};

export const clearAuth = (): void => {
  removeAccessToken();
};
