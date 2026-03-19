export type MemoPosition = {
  x: number;
  y: number;
};

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
  positions: MemoPosition;
  createdAt?: string;
  updatedAt?: string;
  comments: MemoComment[];
};

export type CreateMemoRequest = {
  schemaId: string;
  positions: MemoPosition;
  body: string;
};

export type UpdateMemoRequest = {
  positions: MemoPosition;
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
