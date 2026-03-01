import { isAxiosError } from 'axios';
import { toast } from 'sonner';
import { ErrorCategory, type ApiError } from './types';
import { getErrorMessage } from './error-messages';

type ProblemDetails = {
  detail?: string;
  title?: string;
  status?: number;
  reason?: string;
};

type ErrorResponseData = ApiError | ProblemDetails;

const extractCode = (data: ErrorResponseData): string | undefined => {
  if ('code' in data && data.code) return data.code;
  if ('reason' in data && data.reason) return data.reason;
  return undefined;
};

export const handleApiError = (error: unknown): Promise<never> => {
  if (!isAxiosError<ErrorResponseData>(error)) {
    return Promise.reject(error);
  }

  const errorData = error.response?.data;

  if (!errorData) {
    toast.error('Network error. Please try again.');
    return Promise.reject(error);
  }

  const code = extractCode(errorData);
  const { message, category } = code
    ? getErrorMessage(code)
    : {
        message: 'An unexpected error occurred. Please try again.',
        category: ErrorCategory.USER_FEEDBACK,
      };

  switch (category) {
    case ErrorCategory.SILENT:
      console.error('[Silent Error]', errorData);
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

  return Promise.reject(error);
};
