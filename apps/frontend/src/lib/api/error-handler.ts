import { isAxiosError } from 'axios';
import { toast } from 'sonner';
import { ErrorCategory, type ApiError, type ErrorResponseData } from './types';
import { getErrorMessage } from './error-messages';

const isApiError = (data: ErrorResponseData): data is ApiError =>
  'category' in data && typeof data.category === 'string';

const extractCode = (data: ErrorResponseData): string | undefined => {
  if (isApiError(data)) return data.code;
  return data.reason;
};

const AUTH_REQUIRED_CODES = new Set([
  'AUTH_AUTHENTICATION_REQUIRED',
  'UNAUTHORIZED',
]);

export const notifyAuthRequired = () => {
  toast.info('Please sign in to continue.');
};

const notifyAutoHandledError = (code: string | undefined, message: string) => {
  if (code && AUTH_REQUIRED_CODES.has(code)) {
    notifyAuthRequired();
    return;
  }

  toast.info(message);
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
      console.error('[Silent Error]', errorData);
      break;

    case ErrorCategory.USER_FEEDBACK:
      toast.error(message);
      break;

    case ErrorCategory.AUTO_HANDLE:
      notifyAutoHandledError(code, message);
      break;

    default:
      toast.error(message);
  }

  return Promise.reject(error);
};
