// 기본 형식
export type ApiResponse<T = unknown> = {
  success: boolean;
  result: T | null;
  error: ApiError | null;
};

export const ErrorCategory = {
  USER_FEEDBACK: 'USER_FEEDBACK',
  SILENT: 'SILENT',
  AUTO_HANDLE: 'AUTO_HANDLE',
} as const;

export type ErrorCategoryType =
  (typeof ErrorCategory)[keyof typeof ErrorCategory];

export type ApiError = {
  code?: string;
  message: string;
  category?: ErrorCategoryType;
  details?: Record<string, unknown>;
};
