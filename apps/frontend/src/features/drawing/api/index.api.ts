import { api } from '@/lib/api/helpers';
import type {
  IndexResponse,
  IndexColumnResponse,
  CreateIndexRequest,
  UpdateIndexNameRequest,
  UpdateIndexTypeRequest,
  UpdateIndexColumnSortDirRequest,
  AddColumnToIndexRequest,
  RemoveColumnFromIndexRequest,
  DeleteIndexRequest,
} from './types/index';
import type { AffectedMappingResponse } from './types/common';

export const createIndexAPI = (data: CreateIndexRequest) =>
  api.post<AffectedMappingResponse>('/indexes', data);

export const getIndexAPI = (indexId: string) =>
  api.get<IndexResponse>(`/indexes/${indexId}`);

export const updateIndexNameAPI = (
  indexId: string,
  data: UpdateIndexNameRequest,
) => api.put<IndexResponse>(`/indexes/${indexId}/name`, data);

export const updateIndexTypeAPI = (
  indexId: string,
  data: UpdateIndexTypeRequest,
) => api.put<IndexResponse>(`/indexes/${indexId}/type`, data);

export const addColumnToIndexAPI = (
  indexId: string,
  data: AddColumnToIndexRequest,
) => api.post<IndexColumnResponse>(`/indexes/${indexId}/columns`, data);

export const updateIndexColumnSortDirAPI = (
  indexId: string,
  indexColumnId: string,
  data: UpdateIndexColumnSortDirRequest,
) =>
  api.put<IndexColumnResponse>(
    `/indexes/${indexId}/columns/${indexColumnId}/sort-dir`,
    data,
  );

export const removeColumnFromIndexAPI = (
  indexId: string,
  columnId: string,
  data: RemoveColumnFromIndexRequest,
) => api.delete<null>(`/indexes/${indexId}/columns/${columnId}`, { data });

export const deleteIndexAPI = (indexId: string, data: DeleteIndexRequest) =>
  api.delete<null>(`/indexes/${indexId}`, { data });
