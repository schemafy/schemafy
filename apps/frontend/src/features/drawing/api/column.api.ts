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

export const createColumnAPI = (data: CreateColumnRequest) =>
  api.post<AffectedMappingResponse>('/columns', data);

export const getColumnAPI = (columnId: string) =>
  api.get<ColumnResponse>(`/columns/${columnId}`);

export const updateColumnNameAPI = (
  columnId: string,
  data: UpdateColumnNameRequest,
) => api.put<ColumnResponse>(`/columns/${columnId}/name`, data);

export const updateColumnTypeAPI = (
  columnId: string,
  data: UpdateColumnTypeRequest,
) => api.put<ColumnResponse>(`/columns/${columnId}/type`, data);

export const updateColumnPositionAPI = (
  columnId: string,
  data: UpdateColumnPositionRequest,
) => api.put<ColumnResponse>(`/columns/${columnId}/position`, data);

export const deleteColumnAPI = (columnId: string, data: DeleteColumnRequest) =>
  api.delete<null>(`/columns/${columnId}`, { data });
