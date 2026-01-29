import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/store/auth.store';
import type { PropsWithChildren } from 'react';
import { observer } from 'mobx-react-lite';

export const RequireAuth = observer(({ children }: PropsWithChildren) => {
  const authStore = useAuthStore();
  const { accessToken, user, isAuthLoading, isInitialized } = authStore;
  const location = useLocation();

  // 로딩 중이거나 아직 초기화가 안 된 경우 (새로고침 직후 등)
  if (isAuthLoading || !isInitialized) {
    return (
      <div className="w-full flex justify-center items-center py-16">
        <div className="h-6 w-6 border-2 border-schemafy-light-gray border-t-black rounded-full animate-spin" />
      </div>
    );
  }

  // 인증 정보가 없으면 로그인 페이지로 이동
  if (!accessToken || !user) {
    return <Navigate to="/signin" replace state={{ from: location }} />;
  }

  return <>{children}</>;
});
