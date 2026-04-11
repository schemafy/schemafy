import { useEffect } from 'react';
import { useNavigate, useSearch } from '@tanstack/react-router';
import { observer } from 'mobx-react-lite';
import { authStore } from '@/store/auth.store';

export const OAuthCallbackPage = observer(() => {
  const navigate = useNavigate();
  const oAuthError = useSearch({
    strict: false,
    select: (search) => {
      const value = (search as { error?: unknown }).error;
      return typeof value === 'string' ? value : null;
    },
  });

  const { isInitialized, isAuthLoading, user } = authStore;

  useEffect(() => {
    if (oAuthError) {
      navigate({
        to: '/signin',
        replace: true,
        search: { oauthError: oAuthError },
      });
      return;
    }
    if (!isInitialized || isAuthLoading) return;

    if (user) {
      navigate({ to: '/', replace: true });
    } else {
      navigate({ to: '/signin', replace: true });
    }
  }, [isInitialized, isAuthLoading, user, navigate, oAuthError]);

  return (
    <div className="flex flex-col items-center justify-center py-16 gap-4">
      <div className="h-6 w-6 border-2 border-schemafy-light-gray border-t-black rounded-full animate-spin" />
      <p className="text-sm text-schemafy-dark-gray">
        GitHub 로그인 처리 중...
      </p>
    </div>
  );
});
