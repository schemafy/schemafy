import { api } from '@/lib/api/helpers';
import type {
  TableResponse,
  TableDetailResponse,
  CreateTableRequest,
  UpdateTableNameRequest,
  DeleteTableRequest,
} from './types/table';
import type { AffectedMappingResponse } from './types/common';

export const createTable = (data: CreateTableRequest, extra?: string) =>
  api.post<AffectedMappingResponse>('/api/tables', data, {
    params: extra ? { extra } : undefined,
  });

export const getTable = (tableId: string) =>
  api.get<TableDetailResponse>(`/api/tables/${tableId}`);

export const getTableColumnList = (tableId: string) =>
  api.get<TableDetailResponse['columns']>(`/api/tables/${tableId}/columns`);

export const getTableRelationshipList = (tableId: string) =>
  api.get<TableDetailResponse['relationships']>(
    `/api/tables/${tableId}/relationships`,
  );

export const getTableIndexList = (tableId: string) =>
  api.get<TableDetailResponse['indexes']>(`/api/tables/${tableId}/indexes`);

export const getTableConstraintList = (tableId: string) =>
  api.get<TableDetailResponse['constraints']>(
    `/api/tables/${tableId}/constraints`,
  );

export const updateTableName = (
  tableId: string,
  data: UpdateTableNameRequest,
) => api.put<TableResponse>(`/api/tables/${tableId}/name`, data);

export const deleteTable = (tableId: string, data: DeleteTableRequest) =>
  api.delete<null>(`/api/tables/${tableId}`, { data });
