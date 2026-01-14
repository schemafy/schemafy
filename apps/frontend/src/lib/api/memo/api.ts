import { bffClient } from '../bff-client';
import type { ApiResponse } from '../types';
import type {
  Memo,
  MemoComment,
  CreateMemoRequest,
  UpdateMemoRequest,
  CreateMemoCommentRequest,
  UpdateMemoCommentRequest,
} from './types';

export const createMemo = async (
  data: CreateMemoRequest,
): Promise<ApiResponse<Memo>> => {
  const response = await bffClient.post<ApiResponse<Memo>>(
    '/api/v1.0/memos',
    data,
  );
  return response.data;
};

export const getMemo = async (memoId: string): Promise<ApiResponse<Memo>> => {
  const response = await bffClient.get<ApiResponse<Memo>>(
    `/api/v1.0/memos/${memoId}`,
  );
  return response.data;
};

export const getSchemaMemos = async (
  schemaId: string,
): Promise<ApiResponse<Memo[]>> => {
  const response = await bffClient.get<ApiResponse<Memo[]>>(
    `/api/v1.0/schemas/${schemaId}/memos`,
  );
  return response.data;
};

export const getSchemaMemosWithComments = async (
  schemaId: string,
): Promise<ApiResponse<Memo[]>> => {
  const response = await bffClient.get<ApiResponse<Memo[]>>(
    `/api/v1.0/schemas/${schemaId}/memos-with-comments`,
  );
  return response.data;
};

export const updateMemo = async (
  memoId: string,
  data: UpdateMemoRequest,
): Promise<ApiResponse<Memo>> => {
  const response = await bffClient.put<ApiResponse<Memo>>(
    `/api/v1.0/memos/${memoId}`,
    data,
  );
  return response.data;
};

export const deleteMemo = async (
  memoId: string,
): Promise<ApiResponse<null>> => {
  const response = await bffClient.delete<ApiResponse<null>>(
    `/api/v1.0/memos/${memoId}`,
  );
  return response.data;
};

export const createMemoComment = async (
  memoId: string,
  data: CreateMemoCommentRequest,
): Promise<ApiResponse<MemoComment>> => {
  const response = await bffClient.post<ApiResponse<MemoComment>>(
    `/api/v1.0/memos/${memoId}/comments`,
    data,
  );
  return response.data;
};

export const getMemoComments = async (
  memoId: string,
): Promise<ApiResponse<MemoComment[]>> => {
  const response = await bffClient.get<ApiResponse<MemoComment[]>>(
    `/api/v1.0/memos/${memoId}/comments`,
  );
  return response.data;
};

export const updateMemoComment = async (
  memoId: string,
  commentId: string,
  data: UpdateMemoCommentRequest,
): Promise<ApiResponse<MemoComment>> => {
  const response = await bffClient.put<ApiResponse<MemoComment>>(
    `/api/v1.0/memos/${memoId}/comments/${commentId}`,
    data,
  );
  return response.data;
};

export const deleteMemoComment = async (
  memoId: string,
  commentId: string,
): Promise<ApiResponse<null>> => {
  const response = await bffClient.delete<ApiResponse<null>>(
    `/api/v1.0/memos/${memoId}/comments/${commentId}`,
  );
  return response.data;
};
