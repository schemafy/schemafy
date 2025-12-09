import { api } from '@/lib/api/helpers';
import type {
  ColumnResponse,
  CreateColumnRequest,
  UpdateColumnNameRequest,
  UpdateColumnTypeRequest,
  UpdateColumnPositionRequest,
  DeleteColumnRequest,
} from './types/column';
import type { AffectedMappingResponse } from './types/common';

export const createColumn = (data: CreateColumnRequest) =>
  api.post<AffectedMappingResponse>('/api/columns', data);

export const getColumn = (columnId: string) =>
  api.get<ColumnResponse>(`/api/columns/${columnId}`);

export const updateColumnName = (
  columnId: string,
  data: UpdateColumnNameRequest,
) => api.put<ColumnResponse>(`/api/columns/${columnId}/name`, data);

export const updateColumnType = (
  columnId: string,
  data: UpdateColumnTypeRequest,
) => api.put<ColumnResponse>(`/api/columns/${columnId}/type`, data);

export const updateColumnPosition = (
  columnId: string,
  data: UpdateColumnPositionRequest,
) => api.put<ColumnResponse>(`/api/columns/${columnId}/position`, data);

export const deleteColumn = (columnId: string, data: DeleteColumnRequest) =>
  api.delete<null>(`/api/columns/${columnId}`, { data });
