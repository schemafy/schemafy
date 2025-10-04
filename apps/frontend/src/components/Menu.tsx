import { useState, useRef, useEffect, type ReactNode } from 'react';
import { cn } from '@/lib';

interface MenuProps {
  trigger: ReactNode;
  children: ReactNode;
  className?: string;
}

interface MenuItemProps {
  onClick: () => void;
  children: ReactNode;
  variant?: 'default' | 'destructive';
  className?: string;
}

export const Menu = ({ trigger, children, className }: MenuProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div ref={menuRef} className={cn('relative', className)}>
      <div onClick={() => setIsOpen(!isOpen)}>{trigger}</div>

      {isOpen && (
        <div className="absolute right-0 bg-schemafy-bg border border-schemafy-light-gray rounded-lg shadow-lg z-50">
          {children}
        </div>
      )}
    </div>
  );
};

export const MenuItem = ({
  onClick,
  children,
  variant = 'default',
  className,
}: MenuItemProps) => {
  return (
    <button
      onClick={onClick}
      className={cn(
        'px-4 py-2 text-left font-overline-sm hover:bg-schemafy-secondary transition-colors',
        variant === 'destructive' && 'text-schemafy-destructive',
        className,
      )}
    >
      {children}
    </button>
  );
};
