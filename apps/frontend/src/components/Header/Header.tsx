import { Link } from 'react-router-dom';
import { cn } from '@/lib';
import { logoImg } from '@/assets';
import { LandingContents, CanvasContents } from './Contents';
import { UserMenu } from './UserMenu';
import { useAuthStore } from '@/store';
import { useMemo } from 'react';

export const Header = ({ isCanvasPage }: { isCanvasPage: boolean }) => {
  const accessToken = useAuthStore((s) => s.accessToken);
  const user = useAuthStore((s) => s.user);
  const isAuthLoading = useAuthStore((s) => s.isAuthLoading);

  const contents = useMemo(() => {
    if (isCanvasPage) return <CanvasContents />;
    if (isAuthLoading)
      return (
        <div className="h-5 w-5 border-2 border-schemafy-light-gray border-t-black rounded-full animate-spin" />
      );
    if (accessToken && user) return <UserMenu />;
    return <LandingContents />;
  }, [isCanvasPage, isAuthLoading, accessToken, user]);

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
};
