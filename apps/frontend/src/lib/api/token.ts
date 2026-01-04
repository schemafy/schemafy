import { AuthStore } from '../../store/auth.store';

export const setAccessToken = (token: string | null) => {
  AuthStore.getInstance().setAccessToken(token);
};

export const getAccessToken = () => {
  return AuthStore.getInstance().accessToken;
};

export const removeAccessToken = () => {
  AuthStore.getInstance().clearAccessToken();
};

export const clearAuth = (): void => {
  removeAccessToken();
};
