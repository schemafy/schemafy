import { api } from '@/lib/api/helpers';
import type {
  TableResponse,
  TableDetailResponse,
  CreateTableRequest,
  UpdateTableNameRequest,
  UpdateTableExtraRequest,
  DeleteTableRequest,
} from './types/table';
import type { AffectedMappingResponse } from './types/common';

export const createTableAPI = (data: CreateTableRequest, extra?: string) =>
  api.post<AffectedMappingResponse>('/tables', data, {
    params: extra ? { extra } : undefined,
  });

export const getTableAPI = (tableId: string) =>
  api.get<TableDetailResponse>(`/tables/${tableId}`);

export const getTableColumnListAPI = (tableId: string) =>
  api.get<TableDetailResponse['columns']>(`/tables/${tableId}/columns`);

export const getTableRelationshipListAPI = (tableId: string) =>
  api.get<TableDetailResponse['relationships']>(
    `/tables/${tableId}/relationships`,
  );

export const getTableIndexListAPI = (tableId: string) =>
  api.get<TableDetailResponse['indexes']>(`/tables/${tableId}/indexes`);

export const getTableConstraintListAPI = (tableId: string) =>
  api.get<TableDetailResponse['constraints']>(`/tables/${tableId}/constraints`);

export const updateTableNameAPI = (
  tableId: string,
  data: UpdateTableNameRequest,
) => api.put<TableResponse>(`/tables/${tableId}/name`, data);

export const updateTableExtraAPI = (
  tableId: string,
  data: UpdateTableExtraRequest,
) => api.put<TableResponse>(`/tables/${tableId}/extra`, data);

export const deleteTableAPI = (tableId: string, data: DeleteTableRequest) =>
  api.delete<null>(`/tables/${tableId}`, { data });
