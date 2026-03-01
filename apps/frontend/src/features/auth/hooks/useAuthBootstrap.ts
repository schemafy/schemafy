import { useEffect } from 'react';
import { authStore } from '@/store/auth.store';
import { getMyInfo, refreshToken } from '@/features/auth/api';

export const useAuthBootstrap = () => {
  useEffect(() => {
    const bootstrapAuth = async () => {
      try {
        authStore.setAuthLoading(true);
        await refreshToken();

        const user = await getMyInfo();
        authStore.setUser(user);
      } catch {
        authStore.clearAuth();
      } finally {
        authStore.setAuthLoading(false);
        authStore.setInitialized(true);
      }
    };
    bootstrapAuth();
  }, []);
};
