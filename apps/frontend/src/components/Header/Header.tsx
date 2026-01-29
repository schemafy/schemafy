import { Link } from 'react-router-dom';
import { cn } from '@/lib';
import { logoImg } from '@/assets';
import { useAuthStore } from '@/store/auth.store';
import { useMemo } from 'react';
import { observer } from 'mobx-react-lite';
import { CanvasHeader } from './CanvasHeader';
import { DashboardHeader } from './DashboardHeader';
import { LandingHeader } from './LandingHeader';

export const Header = observer(
  ({ isCanvasPage }: { isCanvasPage: boolean }) => {
    const authStore = useAuthStore();

    const { isAuthLoading, accessToken, user, isInitialized } = authStore;

    const contents = useMemo(() => {
      if (isCanvasPage) return <CanvasHeader />;
      if (isAuthLoading || !isInitialized)
        return (
          <div className="h-5 w-5 border-2 border-schemafy-light-gray border-t-black rounded-full animate-spin" />
        );
      if (accessToken && user) return <DashboardHeader />;
      return <LandingHeader />;
    }, [isCanvasPage, isAuthLoading, accessToken, user, isInitialized]);

    return (
      <header className="w-full border-b border-schemafy-light-gray flex justify-center sticky">
        <div
          className={cn(
            !isCanvasPage && 'max-w-[1280px]',
            'w-full transition-all duration-200 z-50 flex items-center px-10 py-3 justify-between',
          )}
        >
          <Link to="/">
            <div className="flex items-center gap-4">
              <img src={logoImg} alt="schemafy-logo" className="w-4 h-4" />
              <h1 className="font-heading-md">Schemafy</h1>
            </div>
          </Link>
          {contents}
        </div>
      </header>
    );
  },
);
