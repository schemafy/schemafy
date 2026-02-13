import { QueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { isAxiosError } from 'axios';
import { ErrorCategory, type ApiResponse } from '../api/types';

const handleMutationError = (error: Error) => {
  if (!isAxiosError<ApiResponse<unknown>>(error)) {
    toast.error('An unexpected error occurred. Please try again.');
    return;
  }

  const apiError = error.response?.data?.error;

  if (!apiError) {
    toast.error('Network error. Please try again.');
    return;
  }

  const { message, category = ErrorCategory.USER_FEEDBACK } = apiError;

  switch (category) {
    case ErrorCategory.SILENT:
      console.error('[Silent Error]', apiError);
      break;

    case ErrorCategory.USER_FEEDBACK:
      toast.error(message);
      break;

    case ErrorCategory.AUTO_HANDLE:
      toast.info(message);
      break;

    default:
      toast.error(message);
  }
};

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60,
      retry: 1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      onError: handleMutationError,
    },
  },
});
