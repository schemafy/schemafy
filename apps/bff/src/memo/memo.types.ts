export type MemoComment = {
  id: string;
  memoId: string;
  author: Author;
  body: string;
  createdAt?: string;
  updatedAt?: string;
};

export type Memo = {
  id: string;
  schemaId: string;
  author: Author;
  positions: string;
  createdAt?: string;
  updatedAt?: string;
  comments: MemoComment[];
};

export type CreateMemoRequest = {
  schemaId: string;
  positions: string;
  body: string;
};

export type UpdateMemoRequest = {
  positions: string;
};

export type CreateMemoCommentRequest = {
  body: string;
};

export type UpdateMemoCommentRequest = {
  body: string;
};

type Author = {
  id: string;
  name: string;
};

export type ApiError = {
  code?: string;
  message: string;
  details?: Record<string, unknown>;
};

export type ApiResponse<T = unknown> = {
  success: boolean;
  result: T | null;
  error: ApiError | null;
};
