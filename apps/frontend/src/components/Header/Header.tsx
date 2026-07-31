import { Link } from '@tanstack/react-router';
import { cn } from '@/lib';
import { logoImg } from '@/assets';
import { authStore } from '@/store/auth.store';
import { useMemo } from 'react';
import { observer } from 'mobx-react-lite';
import { LoadingSpinner } from '@/components';
import { CanvasHeader } from './CanvasHeader';
import { DashboardHeader } from './DashboardHeader';
import { LandingHeader } from './LandingHeader';

export const Header = observer(
  ({ isCanvasPage }: { isCanvasPage: boolean }) => {
    const { isAuthLoading, accessToken, user, isInitialized } = authStore;

    const contents = useMemo(() => {
      if (isCanvasPage) return <CanvasHeader />;
      if (isAuthLoading || !isInitialized)
        return <LoadingSpinner className="h-5 w-5" />;
      if (accessToken && user) return <DashboardHeader />;
      return <LandingHeader />;
    }, [isCanvasPage, isAuthLoading, accessToken, user, isInitialized]);

    return (
      <header className="sticky top-0 z-50 flex w-full justify-center border-b border-schemafy-glass-border bg-schemafy-panel/90 backdrop-blur-xl">
        <div
          className={cn(
            !isCanvasPage && 'max-w-[1280px]',
            'z-50 flex w-full min-w-0 items-center justify-between gap-3 px-4 py-3 transition-all duration-200 sm:gap-4 sm:px-6 lg:px-10',
          )}
        >
          <Link to="/" className="shrink-0">
            <div className="flex items-center gap-3">
              <img
                src={logoImg}
                alt="schemafy-logo"
                className="h-4 w-4 dark:invert"
              />
              <h1 className="hidden font-heading-sm text-schemafy-text sm:block">
                Schemafy
              </h1>
            </div>
          </Link>
          <div className="flex min-w-0 flex-1 justify-end">{contents}</div>
        </div>
      </header>
    );
  },
);
