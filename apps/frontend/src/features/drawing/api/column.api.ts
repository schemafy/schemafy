import { bffClient } from '@/lib/api/bff-client';
import type { ApiResponse } from '@/lib/api/types';
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
  const { data: res } = await bffClient.post<
    ApiResponse<MutationResponse<ColumnResponse>>
  >('/columns', data);
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const getColumn = async (columnId: string): Promise<ColumnResponse> => {
  const { data } = await bffClient.get<ApiResponse<ColumnResponse>>(
    `/columns/${columnId}`,
  );
  if (!data.success || !data.result) {
    throw data.error;
  }
  return data.result;
};

export const getColumnsByTableId = async (
  tableId: string,
): Promise<ColumnResponse[]> => {
  const { data } = await bffClient.get<ApiResponse<ColumnResponse[]>>(
    `/tables/${tableId}/columns`,
  );
  if (!data.success || !data.result) {
    throw data.error;
  }
  return data.result;
};

export const changeColumnName = async (
  columnId: string,
  data: ChangeColumnNameRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/columns/${columnId}/name`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const changeColumnType = async (
  columnId: string,
  data: ChangeColumnTypeRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/columns/${columnId}/type`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const changeColumnMeta = async (
  columnId: string,
  data: ChangeColumnMetaRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/columns/${columnId}/meta`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const changeColumnPosition = async (
  columnId: string,
  data: ChangeColumnPositionRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/columns/${columnId}/position`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const deleteColumn = async (
  columnId: string,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.delete<ApiResponse<MutationResponse>>(
    `/columns/${columnId}`,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};
