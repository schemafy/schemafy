import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/store';
import type { PropsWithChildren } from 'react';

export const RequireAuth = ({ children }: PropsWithChildren) => {
  const accessToken = useAuthStore((s) => s.accessToken);
  const user = useAuthStore((s) => s.user);
  const isAuthLoading = useAuthStore((s) => s.isAuthLoading);
  const location = useLocation();

  // 로딩 중에는 헤더의 스피너가 노출되므로, 여기서는 그대로 children을 잠시 덮어두는 간단 스켈레톤만 표시
  if (isAuthLoading) {
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
};
