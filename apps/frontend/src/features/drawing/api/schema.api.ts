import { api } from '@/lib/api/helpers';
import type {
  SchemaResponse,
  SchemaDetailResponse,
  CreateSchemaRequest,
  UpdateSchemaNameRequest,
  DeleteSchemaRequest,
} from './types/schema';
import type { AffectedMappingResponse } from './types/common';

export const createSchema = (data: CreateSchemaRequest) =>
  api.post<AffectedMappingResponse>('/schemas', data);

export const getSchema = (schemaId: string) =>
  api.get<SchemaDetailResponse>(`/schemas/${schemaId}`);

export const getSchemaTableList = (schemaId: string) =>
  api.get<SchemaDetailResponse['tables']>(`/schemas/${schemaId}/tables`);

export const updateSchemaName = (
  schemaId: string,
  data: UpdateSchemaNameRequest,
) => api.put<SchemaResponse>(`/schemas/${schemaId}/name`, data);

export const deleteSchema = (schemaId: string, data: DeleteSchemaRequest) =>
  api.delete(`/schemas/${schemaId}`, { data });
