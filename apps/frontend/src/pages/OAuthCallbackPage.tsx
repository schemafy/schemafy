import { useEffect } from 'react';
import { useNavigate, useSearch } from '@tanstack/react-router';
import { observer } from 'mobx-react-lite';
import { authStore } from '@/store/auth.store';
import { LoadingState } from '@/components';

export const OAuthCallbackPage = observer(() => {
  const navigate = useNavigate();
  const { error: oAuthError } = useSearch({ from: '/oauth/callback' });

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
      navigate({ to: '/signin', replace: true, search: { oauthError: null } });
    }
  }, [isInitialized, isAuthLoading, user, navigate, oAuthError]);

  return <LoadingState className="py-16" label="GitHub 로그인 처리 중..." />;
});
