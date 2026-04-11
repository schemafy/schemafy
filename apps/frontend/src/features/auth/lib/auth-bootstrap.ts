import { getMyInfo, refreshToken } from '@/features/auth/api';
import { authStore } from '@/store/auth.store';

let authBootstrapPromise: Promise<boolean> | null = null;

export const isAuthenticated = () => {
  return Boolean(authStore.accessToken && authStore.user);
};

export const ensureAuthInitialized = async () => {
  if (authStore.isInitialized) {
    return isAuthenticated();
  }

  if (!authBootstrapPromise) {
    authBootstrapPromise = (async () => {
      try {
        authStore.setAuthLoading(true);
        await refreshToken();

        const user = await getMyInfo();
        authStore.setUser(user);

        return true;
      } catch {
        authStore.clearAuth();
        return false;
      } finally {
        authStore.setAuthLoading(false);
        authStore.setInitialized(true);
        authBootstrapPromise = null;
      }
    })();
  }

  return authBootstrapPromise;
};
