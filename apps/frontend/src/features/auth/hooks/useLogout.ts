import { useCallback } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { toast } from 'sonner';

import { logout } from '@/features/auth/api';
import { clearAuthSession } from '@/features/auth/lib/auth-session';

export const useLogout = () => {
  const navigate = useNavigate();

  return useCallback(async () => {
    try {
      await logout();
      clearAuthSession();
      await navigate({ to: '/signin', search: { oauthError: null } });
    } catch {
      toast.error('Failed to sign out. Please try again.');
    }
  }, [navigate]);
};
