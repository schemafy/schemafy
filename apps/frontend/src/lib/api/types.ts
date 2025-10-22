// 기본 형식
export interface ApiResponse<T = unknown> {
  success: boolean;
  result: T | null;
  error: ApiError | null;
}

export interface ApiError {
  code?: string;
  message: string;
  details?: Record<string, unknown>;
}
