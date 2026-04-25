import { queryClient } from '@/lib';
import { authStore } from '@/store/auth.store';

export const clearAuthSession = () => {
  authStore.clearAuth();
  queryClient.clear();
};
