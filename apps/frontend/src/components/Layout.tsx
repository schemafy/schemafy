import { Outlet, useRouterState } from '@tanstack/react-router';
import { cn } from '@/lib';
import { Header } from './Header';
import { Footer } from './Footer';

export const Layout = () => {
  const pathname = useRouterState({
    select: (state) => state.location.pathname,
  });
  const isCanvasPage = pathname.startsWith('/project/');
  const isWorkspacePage = pathname.startsWith('/workspace');

  return (
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
        <Outlet />
      </main>
      {!isCanvasPage && !isWorkspacePage && <Footer />}
    </div>
  );
};
