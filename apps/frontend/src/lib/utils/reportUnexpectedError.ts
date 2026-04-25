import { isAxiosError } from 'axios';
import { toast } from 'sonner';

interface ReportUnexpectedErrorOptions {
  allowAxios?: boolean;
  context?: string;
  userMessage?: string;
  toastForUnhandledAxios?: boolean;
}

type HandledAxiosError = {
  __handledByApiError?: boolean;
};

const toError = (error: unknown, context?: string) => {
  if (error instanceof Error) {
    if (!context) {
      return error;
    }

    const contextualError = new Error(`${context}\n${error.message}`);
    contextualError.stack = error.stack;
    return contextualError;
  }

  const message =
    typeof error === 'string'
      ? error
      : context ?? 'An unexpected error occurred.';

  return new Error(message);
};

export const reportUnexpectedError = (
  error: unknown,
  options: ReportUnexpectedErrorOptions = {},
) => {
  const {
    allowAxios = false,
    context,
    userMessage,
    toastForUnhandledAxios = false,
  } = options;

  if (isAxiosError(error) && !allowAxios) {
    const handledByApiError = (error as HandledAxiosError).__handledByApiError;

    if (!handledByApiError && toastForUnhandledAxios && userMessage) {
      toast.error(userMessage);
    }

    return;
  }

  const errorToReport = toError(error, context);
  const reportErrorFn = (globalThis as { reportError?: (error: Error) => void })
    .reportError;

  if (typeof reportErrorFn === 'function') {
    reportErrorFn(errorToReport);
  }

  if (userMessage) {
    toast.error(userMessage);
  }
};
