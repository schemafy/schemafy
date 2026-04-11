import { apiClient } from '@/lib/api';
import { createErdMutationConfig } from './mutation-request';
import type {
  MutationResponse,
  ColumnResponse,
  CreateColumnRequest,
  ChangeColumnNameRequest,
  ChangeColumnTypeRequest,
  ChangeColumnMetaRequest,
  ChangeColumnPositionRequest,
} from './types';

export const createColumn = async (
  data: CreateColumnRequest,
  schemaId: string,
): Promise<MutationResponse<ColumnResponse>> => {
  const { data: res } = await apiClient.post<MutationResponse<ColumnResponse>>(
    '/columns',
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const getColumn = async (columnId: string): Promise<ColumnResponse> => {
  const { data } = await apiClient.get<ColumnResponse>(`/columns/${columnId}`);
  return data;
};

export const getColumnsByTableId = async (
  tableId: string,
): Promise<ColumnResponse[]> => {
  const { data } = await apiClient.get<ColumnResponse[]>(
    `/tables/${tableId}/columns`,
  );
  return data;
};

export const changeColumnName = async (
  columnId: string,
  data: ChangeColumnNameRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/columns/${columnId}/name`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const changeColumnType = async (
  columnId: string,
  data: ChangeColumnTypeRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/columns/${columnId}/type`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const changeColumnMeta = async (
  columnId: string,
  data: ChangeColumnMetaRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/columns/${columnId}/meta`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const changeColumnPosition = async (
  columnId: string,
  data: ChangeColumnPositionRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/columns/${columnId}/position`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const deleteColumn = async (
  columnId: string,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.delete<MutationResponse>(
    `/columns/${columnId}`,
    createErdMutationConfig(schemaId),
  );
  return res;
};
