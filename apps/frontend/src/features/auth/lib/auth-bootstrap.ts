import { getMyInfo, refreshToken } from '@/features/auth/api';
import { authStore } from '@/store/auth.store';
import { reportUnexpectedError } from '@/lib';

let bootstrapPromise: Promise<boolean> | null = null;

export const ensureAuthInitialized = async (): Promise<boolean> => {
  if (authStore.isInitialized) {
    return Boolean(authStore.accessToken && authStore.user);
  }

  if (!bootstrapPromise) {
    bootstrapPromise = (async () => {
      try {
        authStore.setAuthLoading(true);
        await refreshToken({ errorPolicy: 'suppress-toast' });

        const user = await getMyInfo();
        authStore.setUser(user);

        return true;
      } catch (error) {
        authStore.clearAuth();
        reportUnexpectedError(error, {
          context: 'Failed to restore the auth session during app bootstrap.',
        });

        return false;
      } finally {
        authStore.setAuthLoading(false);
        authStore.setInitialized(true);
        bootstrapPromise = null;
      }
    })();
  }

  return bootstrapPromise;
};
