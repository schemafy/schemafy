import type { PropsWithChildren } from 'react';
import { useLocation } from 'react-router-dom';
import { cn } from '@/lib';
import { Header, CanvasContents, DefaultContents } from './Header';
import { Footer } from './Footer';

export const Layout = ({ children }: PropsWithChildren) => {
  const location = useLocation();
  const isCanvasPage = location.pathname === '/canvas';

  return (
    <div className="flex flex-col min-h-screen bg-schemafy-bg w-screen items-center">
      <Header isCanvasPage={isCanvasPage}>
        {isCanvasPage ? <CanvasContents /> : <DefaultContents />}
      </Header>
      <main
        className={cn(
          'flex-grow w-full',
          isCanvasPage ? 'w-full' : 'max-w-screen-lg',
        )}
      >
        {children}
      </main>
      {!isCanvasPage && <Footer />}
    </div>
  );
};
