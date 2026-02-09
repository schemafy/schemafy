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
  const response = await bffClient.post<ApiResponse<Memo>>('/memos', data);
  return response.data;
};

export const getMemo = async (memoId: string): Promise<ApiResponse<Memo>> => {
  const response = await bffClient.get<ApiResponse<Memo>>(`/memos/${memoId}`);
  return response.data;
};

export const getSchemaMemos = async (
  schemaId: string,
): Promise<ApiResponse<Memo[]>> => {
  const response = await bffClient.get<ApiResponse<Memo[]>>(
    `/schemas/${schemaId}/memos`,
  );
  return response.data;
};

export const getSchemaMemosWithComments = async (
  schemaId: string,
): Promise<ApiResponse<Memo[]>> => {
  const response = await bffClient.get<ApiResponse<Memo[]>>(
    `/schemas/${schemaId}/memos-with-comments`,
  );
  return response.data;
};

export const updateMemo = async (
  memoId: string,
  data: UpdateMemoRequest,
): Promise<ApiResponse<Memo>> => {
  const response = await bffClient.put<ApiResponse<Memo>>(
    `/memos/${memoId}`,
    data,
  );
  return response.data;
};

export const deleteMemo = async (
  memoId: string,
): Promise<ApiResponse<null>> => {
  const response = await bffClient.delete<ApiResponse<null>>(
    `/memos/${memoId}`,
  );
  return response.data;
};

export const createMemoComment = async (
  memoId: string,
  data: CreateMemoCommentRequest,
): Promise<ApiResponse<MemoComment>> => {
  const response = await bffClient.post<ApiResponse<MemoComment>>(
    `/memos/${memoId}/comments`,
    data,
  );
  return response.data;
};

export const getMemoComments = async (
  memoId: string,
): Promise<ApiResponse<MemoComment[]>> => {
  const response = await bffClient.get<ApiResponse<MemoComment[]>>(
    `/memos/${memoId}/comments`,
  );
  return response.data;
};

export const updateMemoComment = async (
  memoId: string,
  commentId: string,
  data: UpdateMemoCommentRequest,
): Promise<ApiResponse<MemoComment>> => {
  const response = await bffClient.put<ApiResponse<MemoComment>>(
    `/memos/${memoId}/comments/${commentId}`,
    data,
  );
  return response.data;
};

export const deleteMemoComment = async (
  memoId: string,
  commentId: string,
): Promise<ApiResponse<null>> => {
  const response = await bffClient.delete<ApiResponse<null>>(
    `/memos/${memoId}/comments/${commentId}`,
  );
  return response.data;
};
