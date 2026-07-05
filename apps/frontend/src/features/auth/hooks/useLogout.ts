import { useCallback } from 'react';
import { useNavigate } from '@tanstack/react-router';

import { logout } from '@/features/auth/api';
import { clearAuthSession } from '@/features/auth/lib/auth-session';
import { reportUnexpectedError } from '@/lib';

export const useLogout = () => {
  const navigate = useNavigate();

  return useCallback(async () => {
    try {
      await logout();
      clearAuthSession();
      await navigate({ to: '/signin', search: { oauthError: null } });
    } catch (error) {
      reportUnexpectedError(error, {
        context: 'Unexpected sign-out failure.',
        userMessage: 'Failed to sign out. Please try again.',
      });
    }
  }, [navigate]);
};
