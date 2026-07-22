import { QueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { isUndoRedoInProgressError } from '@/features/drawing/api/mutation-request';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60,
      retry: 1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      onError: (error) => {
        if (isUndoRedoInProgressError(error)) {
          toast.info('Undo/Redo is in progress...');
        }
      },
    },
  },
});
