import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { observer } from 'mobx-react-lite';
import { authStore } from '@/store/auth.store';

export const OAuthCallbackPage = observer(() => {
  const navigate = useNavigate();

  const { isInitialized, isAuthLoading, user } = authStore;

  useEffect(() => {
    if (!isInitialized || isAuthLoading) return;

    if (user) {
      navigate('/', { replace: true });
    } else {
      navigate('/signin', { replace: true });
    }
  }, [isInitialized, isAuthLoading, user, navigate]);

  return (
    <div className="flex flex-col items-center justify-center py-16 gap-4">
      <div className="h-6 w-6 border-2 border-schemafy-light-gray border-t-black rounded-full animate-spin" />
      <p className="text-sm text-schemafy-dark-gray">
        GitHub 로그인 처리 중...
      </p>
    </div>
  );
});
