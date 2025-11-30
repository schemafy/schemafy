import type { PropsWithChildren } from 'react';
import { useLocation } from 'react-router-dom';
import { cn } from '@/lib';
import { Header } from './Header';
import { Footer } from './Footer';
import { Toaster } from './Toaster';

export const Layout = ({ children }: PropsWithChildren) => {
  const location = useLocation();
  const isCanvasPage = location.pathname === '/canvas';

  return (
    <div className="layout flex flex-col min-h-screen bg-schemafy-bg w-full items-center">
      <Header isCanvasPage={isCanvasPage} />
      <main
        className={cn(
          'flex-grow w-full flex ',
          !isCanvasPage && 'max-w-[960px]',
        )}
      >
        {children}
      </main>
      {!isCanvasPage && <Footer />}
      <Toaster />
    </div>
  );
};
