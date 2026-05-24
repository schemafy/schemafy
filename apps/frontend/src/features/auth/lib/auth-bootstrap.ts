import { getMyInfo, refreshToken } from '@/features/auth/api';
import { authStore } from '@/store/auth.store';
import { reportUnexpectedError } from '@/lib';
import { clearAuthSession } from './auth-session';

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
        await refreshToken({ errorPolicy: 'suppress-toast' });

        const user = await getMyInfo();
        authStore.setUser(user);

        return true;
      } catch (error) {
        clearAuthSession();
        reportUnexpectedError(error, {
          context: 'Failed to restore the auth session during app bootstrap.',
        });
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
