import { bffClient } from '@/lib/api/bff-client';
import type { ApiResponse } from '@/lib/api/types';
import type {
  MutationResponse,
  IndexResponse,
  CreateIndexRequest,
  ChangeIndexNameRequest,
  ChangeIndexTypeRequest,
  IndexColumnResponse,
  AddIndexColumnRequest,
  AddIndexColumnResponse,
  ChangeIndexColumnPositionRequest,
  ChangeIndexColumnSortDirectionRequest,
} from './types';

export const createIndex = async (
  data: CreateIndexRequest,
): Promise<MutationResponse<IndexResponse>> => {
  const { data: res } = await bffClient.post<
    ApiResponse<MutationResponse<IndexResponse>>
  >('/indexes', data);
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const getIndex = async (indexId: string): Promise<IndexResponse> => {
  const { data } = await bffClient.get<ApiResponse<IndexResponse>>(
    `/indexes/${indexId}`,
  );
  if (!data.success || !data.result) {
    throw data.error;
  }
  return data.result;
};

export const getIndexesByTableId = async (
  tableId: string,
): Promise<IndexResponse[]> => {
  const { data } = await bffClient.get<ApiResponse<IndexResponse[]>>(
    `/tables/${tableId}/indexes`,
  );
  if (!data.success || !data.result) {
    throw data.error;
  }
  return data.result;
};

export const changeIndexName = async (
  indexId: string,
  data: ChangeIndexNameRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/indexes/${indexId}/name`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const changeIndexType = async (
  indexId: string,
  data: ChangeIndexTypeRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/indexes/${indexId}/type`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const deleteIndex = async (
  indexId: string,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.delete<ApiResponse<MutationResponse>>(
    `/indexes/${indexId}`,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const getIndexColumns = async (
  indexId: string,
): Promise<IndexColumnResponse[]> => {
  const { data } = await bffClient.get<ApiResponse<IndexColumnResponse[]>>(
    `/indexes/${indexId}/columns`,
  );
  if (!data.success || !data.result) {
    throw data.error;
  }
  return data.result;
};

export const addIndexColumn = async (
  indexId: string,
  data: AddIndexColumnRequest,
): Promise<MutationResponse<AddIndexColumnResponse>> => {
  const { data: res } = await bffClient.post<
    ApiResponse<MutationResponse<AddIndexColumnResponse>>
  >(`/indexes/${indexId}/columns`, data);
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const removeIndexColumn = async (
  indexColumnId: string,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.delete<ApiResponse<MutationResponse>>(
    `/index-columns/${indexColumnId}`,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const getIndexColumn = async (
  indexColumnId: string,
): Promise<IndexColumnResponse> => {
  const { data } = await bffClient.get<ApiResponse<IndexColumnResponse>>(
    `/index-columns/${indexColumnId}`,
  );
  if (!data.success || !data.result) {
    throw data.error;
  }
  return data.result;
};

export const changeIndexColumnPosition = async (
  indexColumnId: string,
  data: ChangeIndexColumnPositionRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/index-columns/${indexColumnId}/position`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const changeIndexColumnSortDirection = async (
  indexColumnId: string,
  data: ChangeIndexColumnSortDirectionRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/index-columns/${indexColumnId}/sort-direction`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};
