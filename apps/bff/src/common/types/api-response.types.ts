export const ErrorCategory = {
  USER_FEEDBACK: 'USER_FEEDBACK',
  SILENT: 'SILENT',
  AUTO_HANDLE: 'AUTO_HANDLE',
} as const;

export type ErrorCategoryType =
  (typeof ErrorCategory)[keyof typeof ErrorCategory];

export type ApiError = {
  code: string;
  message: string;
  category: ErrorCategoryType;
  details?: Record<string, unknown>;
};

export type ApiResponse<T = unknown> = {
  success: boolean;
  result: T | null;
  error: ApiError | null;
};

export const ErrorCode = {
  BAD_REQUEST: 'BAD_REQUEST',
  UNAUTHORIZED: 'UNAUTHORIZED',
  FORBIDDEN: 'FORBIDDEN',
  NOT_FOUND: 'NOT_FOUND',
  CONFLICT: 'CONFLICT',
  VALIDATION_ERROR: 'VALIDATION_ERROR',

  INTERNAL_SERVER_ERROR: 'INTERNAL_SERVER_ERROR',
  SERVICE_UNAVAILABLE: 'SERVICE_UNAVAILABLE',
  GATEWAY_TIMEOUT: 'GATEWAY_TIMEOUT',

  BACKEND_UNREACHABLE: 'BACKEND_UNREACHABLE',
  BACKEND_TIMEOUT: 'BACKEND_TIMEOUT',
} as const;

export type ErrorCodeType = (typeof ErrorCode)[keyof typeof ErrorCode];
