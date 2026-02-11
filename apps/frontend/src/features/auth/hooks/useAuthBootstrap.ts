import { useEffect } from 'react';
import { authStore } from '@/store/auth.store';
import { getMyInfo, refreshToken } from '@/lib/api';

export const useAuthBootstrap = () => {
  useEffect(() => {
    const bootstrapAuth = async () => {
      try {
        authStore.setAuthLoading(true);
        await refreshToken();

        const res = await getMyInfo();
        if (res.success && res.result) {
          authStore.setUser(res.result);
        } else {
          authStore.clearAuth();
        }
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
