import { useState, useRef, useEffect } from 'react';
import { cn } from '@/lib';

interface DropdownProps {
  value: string;
  options: string[];
  onChange: (value: string) => void;
  className?: string;
}

export const Dropdown = ({
  value,
  options,
  onChange,
  className,
}: DropdownProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div ref={dropdownRef} className={cn('relative', className)}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 py-2 bg-transparent text-schemafy-text font-heading-xl hover:bg-schemafy-secondary rounded-lg transition-colors"
      >
        <span>{value}</span>
        <svg
          width="12"
          height="8"
          viewBox="0 0 12 8"
          fill="none"
          className={cn('transition-transform', isOpen && 'rotate-180')}
        >
          <path
            d="M1 1L6 6L11 1"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>

      {isOpen && (
        <div className="absolute top-full left-0 mt-2 w-64 bg-schemafy-bg border border-schemafy-light-gray rounded-lg shadow-lg z-50 py-2">
          {options.map((option) => (
            <button
              key={option}
              onClick={() => {
                onChange(option);
                setIsOpen(false);
              }}
              className={cn(
                'w-full px-4 py-2 text-left font-body-md hover:bg-schemafy-secondary transition-colors',
                value === option && 'bg-schemafy-secondary',
              )}
            >
              {option}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
