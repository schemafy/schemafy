import type { PropsWithChildren } from 'react';
import { matchPath, useLocation } from 'react-router-dom';
import { cn } from '@/lib';
import { Header } from './Header';
import { Footer } from './Footer';
import { Toaster } from './Toaster';

export const Layout = ({ children }: PropsWithChildren) => {
  const location = useLocation();
  const isCanvasPage = location.pathname.startsWith('/canvas');
  const isWorkspacePage = location.pathname.startsWith('/workspace');
  const canvasMatch = matchPath('/canvas/:projectId', location.pathname);
  const projectId = canvasMatch?.params.projectId ?? '';

  return (
    <div className="layout flex flex-col min-h-screen bg-schemafy-bg w-full items-center">
      <Header isCanvasPage={isCanvasPage} projectId={projectId} />
      <main
        className={cn(
          'flex-grow w-full flex',
          !isCanvasPage && !isWorkspacePage && 'max-w-[960px]',
        )}
      >
        {children}
      </main>
      {!isCanvasPage && !isWorkspacePage && <Footer />}
      <Toaster />
    </div>
  );
};
