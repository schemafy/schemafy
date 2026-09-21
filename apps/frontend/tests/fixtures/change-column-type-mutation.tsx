import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createRoot } from 'react-dom/client';

import { useChangeColumnType } from '@/features/drawing/hooks/useColumnMutations';
import { authStore } from '@/store/auth.store';

const queryClient = new QueryClient({
  defaultOptions: {
    mutations: { retry: false },
    queries: { retry: false },
  },
});

export const MutationFixture = () => {
  const mutation = useChangeColumnType('schema-1');

  return (
    <>
      <button
        type="button"
        data-testid="precision-only"
        onClick={() =>
          mutation.mutate({
            columnId: 'column-1',
            data: {
              dataType: 'DECIMAL',
              precision: 10,
              scale: null,
            },
          })
        }
      >
        Precision only
      </button>
      <button
        type="button"
        data-testid="precision-scale"
        onClick={() =>
          mutation.mutate({
            columnId: 'column-1',
            data: {
              dataType: 'DECIMAL',
              precision: 10,
              scale: 2,
            },
          })
        }
      >
        Precision and scale
      </button>
    </>
  );
};

authStore.setAccessToken('test-access-token');
createRoot(document.getElementById('root')!).render(
  <QueryClientProvider client={queryClient}>
    <MutationFixture />
  </QueryClientProvider>,
);
