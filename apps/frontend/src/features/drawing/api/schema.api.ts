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
  api.post<AffectedMappingResponse>('/api/schemas', data);

export const getSchema = (schemaId: string) =>
  api.get<SchemaDetailResponse>(`/api/schemas/${schemaId}`);

export const getSchemaTableList = (schemaId: string) =>
  api.get<SchemaDetailResponse['tables']>(`/api/schemas/${schemaId}/tables`);

export const updateSchemaName = (
  schemaId: string,
  data: UpdateSchemaNameRequest,
) => api.put<SchemaResponse>(`/api/schemas/${schemaId}/name`, data);

export const deleteSchema = (schemaId: string, data: DeleteSchemaRequest) =>
  api.delete<null>(`/api/schemas/${schemaId}`, { data });
