import { apiClient } from '@/lib/api';
import { createErdMutationConfig } from './mutation-request';
import type {
  MutationResponse,
  TableResponse,
  CreateTableRequest,
  ChangeTableNameRequest,
  ChangeTableMetaRequest,
  ChangeTableExtraRequest,
  SchemaSnapshotsResponse,
  TableSnapshotResponse,
} from './types';

export const createTable = async (
  data: CreateTableRequest,
): Promise<MutationResponse<TableResponse>> => {
  const { data: res } = await apiClient.post<MutationResponse<TableResponse>>(
    '/tables',
    data,
    createErdMutationConfig(data.schemaId),
  );
  return res;
};

export const getTable = async (tableId: string): Promise<TableResponse> => {
  const { data } = await apiClient.get<TableResponse>(`/tables/${tableId}`);
  return data;
};

export const getTablesBySchemaId = async (
  schemaId: string,
): Promise<TableResponse[]> => {
  const { data } = await apiClient.get<TableResponse[]>(
    `/schemas/${schemaId}/tables`,
  );
  return data;
};

export const getTableSnapshot = async (
  tableId: string,
): Promise<TableSnapshotResponse> => {
  const { data } = await apiClient.get<TableSnapshotResponse>(
    `/tables/${tableId}/snapshot`,
  );
  return data;
};

export const getTableSnapshots = async (
  tableIds: string[],
): Promise<Record<string, TableSnapshotResponse>> => {
  const { data } = await apiClient.get<Record<string, TableSnapshotResponse>>(
    '/tables/snapshots',
    { params: { tableIds } },
  );
  return data;
};

export const changeTableName = async (
  tableId: string,
  data: ChangeTableNameRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/tables/${tableId}/name`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const changeTableMeta = async (
  tableId: string,
  data: ChangeTableMetaRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/tables/${tableId}/meta`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const changeTableExtra = async (
  tableId: string,
  data: ChangeTableExtraRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/tables/${tableId}/extra`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const deleteTable = async (
  tableId: string,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.delete<MutationResponse>(
    `/tables/${tableId}`,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const getSchemaWithSnapshots = async (
  schemaId: string,
): Promise<SchemaSnapshotsResponse> => {
  const { data } = await apiClient.get<SchemaSnapshotsResponse>(
    `/schemas/${schemaId}/snapshots`,
  );
  return data;
};
