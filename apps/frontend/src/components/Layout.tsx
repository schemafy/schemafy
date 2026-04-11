import { Outlet, useRouterState } from '@tanstack/react-router';
import { cn } from '@/lib';
import { Header } from './Header';
import { Footer } from './Footer';
import { Toaster } from './Toaster';

export const Layout = () => {
  const pathname = useRouterState({
    select: (state) => state.location.pathname,
  });
  const isCanvasPage = pathname.startsWith('/project/');
  const isWorkspacePage = pathname.startsWith('/workspace');

  return (
    <div className="layout flex flex-col min-h-screen bg-schemafy-bg w-full items-center">
      <Header isCanvasPage={isCanvasPage} />
      <main
        className={cn(
          'grow w-full flex',
          !isCanvasPage && !isWorkspacePage && 'max-w-240',
        )}
      >
        <Outlet />
      </main>
      {!isCanvasPage && !isWorkspacePage && <Footer />}
      <Toaster />
    </div>
  );
};
