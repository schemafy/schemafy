import type { PropsWithChildren } from 'react';
import { useLocation } from 'react-router-dom';
import { cn } from '@/lib';
import { Header } from './Header';
import { Footer } from './Footer';

export const Layout = ({ children }: PropsWithChildren) => {
  const location = useLocation();
  const isCanvasPage = location.pathname === '/canvas';

  return (
    <div className="flex flex-col min-h-screen bg-schemafy-bg w-screen items-center">
      <Header isCanvasPage={isCanvasPage} />
      <main
        className={cn('flex-grow w-full', !isCanvasPage && 'max-w-[960px]')}
      >
        {children}
      </main>
      {!isCanvasPage && <Footer />}
    </div>
  );
};
