import { Loader2 } from 'lucide-react';
import { cn } from '@/lib';

interface LoadingSpinnerProps {
  className?: string;
}

export const LoadingSpinner = ({ className }: LoadingSpinnerProps) => {
  return (
    <Loader2
      aria-hidden="true"
      className={cn('h-6 w-6 animate-spin text-schemafy-text', className)}
    />
  );
};

interface LoadingStateProps {
  label?: string;
  className?: string;
  spinnerClassName?: string;
}

export const LoadingState = ({
  label,
  className,
  spinnerClassName,
}: LoadingStateProps) => {
  return (
    <div
      role="status"
      aria-live="polite"
      className={cn(
        'flex flex-1 w-full items-center justify-center bg-schemafy-bg',
        className,
      )}
    >
      <div className="flex flex-col items-center gap-3">
        <LoadingSpinner className={spinnerClassName} />
        {label && <span className="text-sm text-schemafy-text">{label}</span>}
      </div>
    </div>
  );
};
