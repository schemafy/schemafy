import { useEffect } from 'react';
import { authStore } from '@/store/auth.store';
import { getMyInfo, refreshToken } from '@/features/auth/api';
import { reportUnexpectedError } from '@/lib';

export const useAuthBootstrap = () => {
  useEffect(() => {
    const bootstrapAuth = async () => {
      try {
        authStore.setAuthLoading(true);
        await refreshToken({ silent: true });

        const user = await getMyInfo();
        authStore.setUser(user);
      } catch (error) {
        authStore.clearAuth();
        reportUnexpectedError(error, {
          context: 'Failed to restore the auth session during app bootstrap.',
        });
      } finally {
        authStore.setAuthLoading(false);
        authStore.setInitialized(true);
      }
    };
    bootstrapAuth();
  }, []);
};
