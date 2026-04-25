import { useEffect } from 'react';
import { authStore } from '@/store/auth.store';
import { getMyInfo, refreshToken } from '@/features/auth/api';
import { reportUnexpectedError } from '@/lib';

export const useAuthBootstrap = () => {
  useEffect(() => {
    const bootstrapAuth = async () => {
      try {
        authStore.setAuthLoading(true);
        await refreshToken();

        const user = await getMyInfo();
        authStore.setUser(user);
      } catch (error) {
        authStore.clearAuth();
        reportUnexpectedError(error, {
          userMessage: 'Failed to restore your session. Please sign in again.',
        });
      } finally {
        authStore.setAuthLoading(false);
        authStore.setInitialized(true);
      }
    };
    bootstrapAuth();
  }, []);
};
