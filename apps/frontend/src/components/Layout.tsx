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
        'layout flex flex-col bg-schemafy-bg w-full items-center',
        isCanvasPage || isWorkspacePage ? 'h-screen overflow-hidden' : 'min-h-screen',
      )}
    >
      <Header isCanvasPage={isCanvasPage} />
      <main
        className={cn(
          'flex-grow min-h-0 w-full flex',
          !isCanvasPage && !isWorkspacePage && 'max-w-[960px]',
        )}
      >
        <Outlet />
      </main>
      {!isCanvasPage && !isWorkspacePage && <Footer />}
    </div>
  );
};
