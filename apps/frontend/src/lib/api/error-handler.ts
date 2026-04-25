import { isAxiosError } from 'axios';
import { toast } from 'sonner';
import { ErrorCategory, type ApiError, type ErrorResponseData } from './types';
import { getErrorMessage } from './error-messages';
import { reportUnexpectedError } from '@/lib';

const isApiError = (data: ErrorResponseData): data is ApiError =>
  'category' in data && typeof data.category === 'string';

const extractCode = (data: ErrorResponseData): string | undefined => {
  if (isApiError(data)) return data.code;
  return data.reason;
};

export const handleApiError = (error: unknown): Promise<never> => {
  if (!isAxiosError<ErrorResponseData>(error)) {
    return Promise.reject(error);
  }

  const errorData = error.response?.data;

  if (!errorData) {
    const { message } = getErrorMessage('NETWORK_ERROR');
    toast.error(message);
    return Promise.reject(error);
  }

  const code = extractCode(errorData);
  const errorInfo = getErrorMessage(code ?? '');
  const category = isApiError(errorData)
    ? errorData.category
    : errorInfo.category;
  const message = errorInfo.message;

  switch (category) {
    case ErrorCategory.SILENT:
      reportUnexpectedError(error, {
        allowAxios: true,
        context: '[Silent API Error]',
      });
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
