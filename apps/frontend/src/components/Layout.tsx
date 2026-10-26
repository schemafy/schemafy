import { Outlet, useRouterState } from '@tanstack/react-router';
import type { ReactNode } from 'react';
import { cn } from '@/lib';
import { Header } from './Header';
import { Footer } from './Footer';

interface AppLayoutProps {
  children: ReactNode;
  isCanvasPage?: boolean;
  isWorkspacePage?: boolean;
}

export const AppLayout = ({
  children,
  isCanvasPage = false,
  isWorkspacePage = false,
}: AppLayoutProps) => (
  <div
    className={cn(
      'layout flex min-h-screen w-full flex-col items-center bg-schemafy-bg',
      !isCanvasPage && 'schemafy-page-gradient',
    )}
  >
    <Header isCanvasPage={isCanvasPage} />
    <main
      className={cn(
        'flex-grow min-h-0 w-full flex',
        !isCanvasPage && !isWorkspacePage && 'max-w-[1120px]',
      )}
    >
      {children}
    </main>
    {!isCanvasPage && !isWorkspacePage && <Footer />}
  </div>
);

export const Layout = () => {
  const pathname = useRouterState({
    select: (state) => state.location.pathname,
  });
  const isCanvasPage = pathname.startsWith('/project/');
  const isWorkspacePage = pathname.startsWith('/workspace');

  if (isCanvasPage) return <Outlet />;

  return (
    <AppLayout isWorkspacePage={isWorkspacePage}>
      <Outlet />
    </AppLayout>
  );
};
