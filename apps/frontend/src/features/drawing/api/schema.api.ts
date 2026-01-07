import { api } from '@/lib/api/helpers';
import type {
  SchemaResponse,
  SchemaDetailResponse,
  CreateSchemaRequest,
  UpdateSchemaNameRequest,
  DeleteSchemaRequest,
} from './types/schema';
import type { AffectedMappingResponse } from './types/common';

export const createSchemaAPI = (data: CreateSchemaRequest) =>
  api.post<AffectedMappingResponse>('/schemas', data);

export const getSchemaAPI = (schemaId: string) =>
  api.get<SchemaDetailResponse>(`/schemas/${schemaId}`);

export const getSchemaTableListAPI = (schemaId: string) =>
  api.get<SchemaDetailResponse['tables']>(`/schemas/${schemaId}/tables`);

export const updateSchemaNameAPI = (
  schemaId: string,
  data: UpdateSchemaNameRequest,
) => api.put<SchemaResponse>(`/schemas/${schemaId}/name`, data);

export const deleteSchemaAPI = (schemaId: string, data: DeleteSchemaRequest) =>
  api.delete(`/schemas/${schemaId}`, { data });
