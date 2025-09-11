import { cn } from '@/lib';
import { logoImg } from '@/assets';
import { Link } from 'react-router-dom';
import type { ReactNode } from 'react';

export const Header = ({
  children,
  isCanvasPage,
}: {
  children: ReactNode;
  isCanvasPage: boolean;
}) => {
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
        {children}
      </div>
    </header>
  );
};
