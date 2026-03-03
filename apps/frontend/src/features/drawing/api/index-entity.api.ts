import { apiClient } from '@/lib/api';
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
  const { data: res } = await apiClient.post<MutationResponse<IndexResponse>>(
    '/indexes',
    data,
  );
  return res;
};

export const getIndex = async (indexId: string): Promise<IndexResponse> => {
  const { data } = await apiClient.get<IndexResponse>(`/indexes/${indexId}`);
  return data;
};

export const getIndexesByTableId = async (
  tableId: string,
): Promise<IndexResponse[]> => {
  const { data } = await apiClient.get<IndexResponse[]>(
    `/tables/${tableId}/indexes`,
  );
  return data;
};

export const changeIndexName = async (
  indexId: string,
  data: ChangeIndexNameRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/indexes/${indexId}/name`,
    data,
  );
  return res;
};

export const changeIndexType = async (
  indexId: string,
  data: ChangeIndexTypeRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/indexes/${indexId}/type`,
    data,
  );
  return res;
};

export const deleteIndex = async (
  indexId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.delete<MutationResponse>(
    `/indexes/${indexId}`,
  );
  return res;
};

export const getIndexColumns = async (
  indexId: string,
): Promise<IndexColumnResponse[]> => {
  const { data } = await apiClient.get<IndexColumnResponse[]>(
    `/indexes/${indexId}/columns`,
  );
  return data;
};

export const addIndexColumn = async (
  indexId: string,
  data: AddIndexColumnRequest,
): Promise<MutationResponse<AddIndexColumnResponse>> => {
  const { data: res } = await apiClient.post<
    MutationResponse<AddIndexColumnResponse>
  >(`/indexes/${indexId}/columns`, data);
  return res;
};

export const removeIndexColumn = async (
  indexColumnId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.delete<MutationResponse>(
    `/index-columns/${indexColumnId}`,
  );
  return res;
};

export const getIndexColumn = async (
  indexColumnId: string,
): Promise<IndexColumnResponse> => {
  const { data } = await apiClient.get<IndexColumnResponse>(
    `/index-columns/${indexColumnId}`,
  );
  return data;
};

export const changeIndexColumnPosition = async (
  indexColumnId: string,
  data: ChangeIndexColumnPositionRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/index-columns/${indexColumnId}/position`,
    data,
  );
  return res;
};

export const changeIndexColumnSortDirection = async (
  indexColumnId: string,
  data: ChangeIndexColumnSortDirectionRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/index-columns/${indexColumnId}/sort-direction`,
    data,
  );
  return res;
};
