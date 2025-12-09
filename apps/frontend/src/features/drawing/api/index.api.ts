import { api } from '@/lib/api/helpers';
import type {
  IndexResponse,
  CreateIndexRequest,
  UpdateIndexNameRequest,
  AddColumnToIndexRequest,
  RemoveColumnFromIndexRequest,
  DeleteIndexRequest,
  IndexColumnResponse,
} from './types/index';
import type { AffectedMappingResponse } from './types/common';

export const createIndex = (data: CreateIndexRequest) =>
  api.post<AffectedMappingResponse>('/api/indexes', data);

export const getIndex = (indexId: string) =>
  api.get<IndexResponse>(`/api/indexes/${indexId}`);

export const updateIndexName = (
  indexId: string,
  data: UpdateIndexNameRequest,
) => api.put<IndexResponse>(`/api/indexes/${indexId}/name`, data);

export const addColumnToIndex = (
  indexId: string,
  data: AddColumnToIndexRequest,
) => api.post<IndexColumnResponse>(`/api/indexes/${indexId}/columns`, data);

export const removeColumnFromIndex = (
  indexId: string,
  columnId: string,
  data: RemoveColumnFromIndexRequest,
) => api.delete<null>(`/api/indexes/${indexId}/columns/${columnId}`, { data });

export const deleteIndex = (indexId: string, data: DeleteIndexRequest) =>
  api.delete<null>(`/api/indexes/${indexId}`, { data });
