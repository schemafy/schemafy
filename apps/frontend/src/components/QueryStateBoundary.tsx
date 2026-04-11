import type { ReactNode } from 'react';
import { Button } from './Button';
import { LoadingState } from './Loading';

interface QueryStateBoundaryProps<TData> {
  data: TData | undefined;
  isPending: boolean;
  isError?: boolean;
  isEmpty?: (data: TData) => boolean;
  loadingLabel?: string;
  errorMessage?: string;
  emptyFallback?: ReactNode;
  onRetry?: () => void;
  children: (data: TData) => ReactNode;
}

export const QueryStateBoundary = <TData,>({
  data,
  isPending,
  isError = false,
  isEmpty,
  loadingLabel,
  errorMessage = 'Failed to load data.',
  emptyFallback,
  onRetry,
  children,
}: QueryStateBoundaryProps<TData>) => {
  if (isPending) {
    return <LoadingState className="min-h-full" label={loadingLabel} />;
  }

  if (isError || !data) {
    return (
      <div className="flex w-full min-h-full flex-col justify-center items-center gap-3">
        <p className="text-sm text-schemafy-dark-gray">{errorMessage}</p>
        {onRetry && (
          <Button variant="secondary" size="sm" onClick={onRetry}>
            Retry
          </Button>
        )}
      </div>
    );
  }

  if (isEmpty?.(data)) {
    return <>{emptyFallback}</>;
  }

  return <>{children(data)}</>;
};
