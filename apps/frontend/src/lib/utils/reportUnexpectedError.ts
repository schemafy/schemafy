import { isAxiosError } from 'axios';
import { toast } from 'sonner';

interface ReportErrorOptions {
  context?: string;
  userMessage?: string;
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

const emitError = (
  error: unknown,
  options: ReportErrorOptions = {},
) => {
  const { context, userMessage } = options;

  const errorToReport = toError(error, context);
  const reportErrorFn = (globalThis as { reportError?: (error: Error) => void })
    .reportError;

  if (typeof reportErrorFn === 'function') {
    reportErrorFn(errorToReport);
  }

  if (import.meta.env.DEV) {
    console.error(errorToReport);
  }

  if (userMessage) {
    toast.error(userMessage);
  }
};

export const reportError = (
  error: unknown,
  options: ReportErrorOptions = {},
) => {
  emitError(error, options);
};

export const reportUnexpectedError = (
  error: unknown,
  options: ReportErrorOptions = {},
) => {
  if (
    isAxiosError(error) &&
    (error as HandledAxiosError).__handledByApiError
  ) {
    return;
  }

  emitError(error, options);
};
