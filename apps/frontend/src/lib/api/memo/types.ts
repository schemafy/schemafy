export type MemoComment = {
  id: string;
  memoId: string;
  authorId: string;
  body: string;
  createdAt?: string;
  updatedAt?: string;
};

export type Memo = {
  id: string;
  schemaId: string;
  authorId: string;
  positions: string;
  createdAt?: string;
  updatedAt?: string;
  comments?: MemoComment[];
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
