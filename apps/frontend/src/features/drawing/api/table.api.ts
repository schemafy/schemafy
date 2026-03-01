import { type ApiResponse, apiClient } from '@/lib/api';
import type {
  MutationResponse,
  TableResponse,
  CreateTableRequest,
  ChangeTableNameRequest,
  ChangeTableMetaRequest,
  ChangeTableExtraRequest,
  TableSnapshotResponse,
} from './types';

export const createTable = async (
  data: CreateTableRequest,
): Promise<MutationResponse<TableResponse>> => {
  const { data: res } = await apiClient.post<
    ApiResponse<MutationResponse<TableResponse>>
  >('/tables', data);
  if (!res.success || !res.result) {
    throw res.error ?? new Error('Unknown API error');
  }
  return res.result;
};

export const getTable = async (tableId: string): Promise<TableResponse> => {
  const { data } = await apiClient.get<ApiResponse<TableResponse>>(
    `/tables/${tableId}`,
  );
  if (!data.success || !data.result) {
    throw data.error ?? new Error('Unknown API error');
  }
  return data.result;
};

export const getTablesBySchemaId = async (
  schemaId: string,
): Promise<TableResponse[]> => {
  const { data } = await apiClient.get<ApiResponse<TableResponse[]>>(
    `/schemas/${schemaId}/tables`,
  );
  if (!data.success || !data.result) {
    throw data.error ?? new Error('Unknown API error');
  }
  return data.result;
};

export const getTableSnapshot = async (
  tableId: string,
): Promise<TableSnapshotResponse> => {
  const { data } = await apiClient.get<ApiResponse<TableSnapshotResponse>>(
    `/tables/${tableId}/snapshot`,
  );
  if (!data.success || !data.result) {
    throw data.error ?? new Error('Unknown API error');
  }
  return data.result;
};

export const getTableSnapshots = async (
  tableIds: string[],
): Promise<Record<string, TableSnapshotResponse>> => {
  const { data } = await apiClient.get<
    ApiResponse<Record<string, TableSnapshotResponse>>
  >('/tables/snapshots', { params: { tableIds } });
  if (!data.success || !data.result) {
    throw data.error ?? new Error('Unknown API error');
  }
  return data.result;
};

export const changeTableName = async (
  tableId: string,
  data: ChangeTableNameRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<ApiResponse<MutationResponse>>(
    `/tables/${tableId}/name`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error ?? new Error('Unknown API error');
  }
  return res.result;
};

export const changeTableMeta = async (
  tableId: string,
  data: ChangeTableMetaRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<ApiResponse<MutationResponse>>(
    `/tables/${tableId}/meta`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error ?? new Error('Unknown API error');
  }
  return res.result;
};

export const changeTableExtra = async (
  tableId: string,
  data: ChangeTableExtraRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<ApiResponse<MutationResponse>>(
    `/tables/${tableId}/extra`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error ?? new Error('Unknown API error');
  }
  return res.result;
};

export const deleteTable = async (
  tableId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.delete<ApiResponse<MutationResponse>>(
    `/tables/${tableId}`,
  );
  if (!res.success || !res.result) {
    throw res.error ?? new Error('Unknown API error');
  }
  return res.result;
};

export const getSchemaWithSnapshots = async (
  schemaId: string,
): Promise<Record<string, TableSnapshotResponse>> => {
  const { data } = await apiClient.get<
    ApiResponse<Record<string, TableSnapshotResponse>>
  >(`/schemas/${schemaId}/snapshots`);
  if (!data.success || !data.result) {
    throw data.error ?? new Error('Unknown API error');
  }
  return data.result;
};
