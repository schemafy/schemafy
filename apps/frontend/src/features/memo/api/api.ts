import { apiClient } from '@/lib/api';
import type {
  Memo,
  MemoComment,
  CreateMemoRequest,
  UpdateMemoRequest,
  CreateMemoCommentRequest,
  UpdateMemoCommentRequest,
} from './types';

export const createMemo = async (data: CreateMemoRequest): Promise<Memo> => {
  const response = await apiClient.post<Memo>('/memos', data);
  return response.data;
};

export const getMemo = async (memoId: string): Promise<Memo> => {
  const response = await apiClient.get<Memo>(`/memos/${memoId}`);
  return response.data;
};

export const getSchemaMemos = async (schemaId: string): Promise<Memo[]> => {
  const response = await apiClient.get<Memo[]>(`/schemas/${schemaId}/memos`);
  return response.data;
};

export const getSchemaMemosWithComments = async (
  schemaId: string,
): Promise<Memo[]> => {
  const response = await apiClient.get<Memo[]>(
    `/schemas/${schemaId}/memos-with-comments`,
  );
  return response.data;
};

export const updateMemo = async (
  memoId: string,
  data: UpdateMemoRequest,
): Promise<Memo> => {
  const response = await apiClient.put<Memo>(`/memos/${memoId}`, data);
  return response.data;
};

export const deleteMemo = async (memoId: string): Promise<null> => {
  const response = await apiClient.delete<null>(`/memos/${memoId}`);
  return response.data;
};

export const createMemoComment = async (
  memoId: string,
  data: CreateMemoCommentRequest,
): Promise<MemoComment> => {
  const response = await apiClient.post<MemoComment>(
    `/memos/${memoId}/comments`,
    data,
  );
  return response.data;
};

export const getMemoComments = async (
  memoId: string,
): Promise<MemoComment[]> => {
  const response = await apiClient.get<MemoComment[]>(
    `/memos/${memoId}/comments`,
  );
  return response.data;
};

export const updateMemoComment = async (
  memoId: string,
  commentId: string,
  data: UpdateMemoCommentRequest,
): Promise<MemoComment> => {
  const response = await apiClient.put<MemoComment>(
    `/memos/${memoId}/comments/${commentId}`,
    data,
  );
  return response.data;
};

export const deleteMemoComment = async (
  memoId: string,
  commentId: string,
): Promise<null> => {
  const response = await apiClient.delete<null>(
    `/memos/${memoId}/comments/${commentId}`,
  );
  return response.data;
};
