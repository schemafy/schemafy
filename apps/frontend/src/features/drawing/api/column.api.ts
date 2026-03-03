import { apiClient } from '@/lib/api';
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
): Promise<MutationResponse<ColumnResponse>> => {
  const { data: res } = await apiClient.post<MutationResponse<ColumnResponse>>(
    '/columns',
    data,
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
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/columns/${columnId}/name`,
    data,
  );
  return res;
};

export const changeColumnType = async (
  columnId: string,
  data: ChangeColumnTypeRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/columns/${columnId}/type`,
    data,
  );
  return res;
};

export const changeColumnMeta = async (
  columnId: string,
  data: ChangeColumnMetaRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/columns/${columnId}/meta`,
    data,
  );
  return res;
};

export const changeColumnPosition = async (
  columnId: string,
  data: ChangeColumnPositionRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/columns/${columnId}/position`,
    data,
  );
  return res;
};

export const deleteColumn = async (
  columnId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.delete<MutationResponse>(
    `/columns/${columnId}`,
  );
  return res;
};
