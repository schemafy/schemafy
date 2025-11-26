// 기본 형식
export type ApiResponse<T = unknown> = {
  success: boolean;
  result: T | null;
  error: ApiError | null;
};

export type ApiError = {
  code?: string;
  message: string;
  details?: Record<string, unknown>;
};
