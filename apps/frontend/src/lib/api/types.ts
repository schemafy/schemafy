export const ErrorCategory = {
  USER_FEEDBACK: 'USER_FEEDBACK',
  SILENT: 'SILENT',
  AUTO_HANDLE: 'AUTO_HANDLE',
} as const;

export type ErrorCategoryType =
  (typeof ErrorCategory)[keyof typeof ErrorCategory];

export type ApiError = {
  code: string;
  category: ErrorCategoryType;
  details?: Record<string, unknown>;
};

type ProblemDetails = {
  type?: string;
  detail?: string;
  title?: string;
  status?: number;
  reason?: string;
};

export type ErrorResponseData = ApiError | ProblemDetails;

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
