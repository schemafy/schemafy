import { apiClient } from '@/lib/api';
import { createErdMutationConfig } from './mutation-request';
import type {
  MutationResponse,
  SchemaResponse,
  CreateSchemaRequest,
  ChangeSchemaNameRequest,
} from './types';

export const createSchema = async (
  data: CreateSchemaRequest,
): Promise<MutationResponse<SchemaResponse>> => {
  const { data: res } = await apiClient.post<MutationResponse<SchemaResponse>>(
    '/schemas',
    data,
    createErdMutationConfig(),
  );
  return res;
};

export const getSchemasByProjectId = async (
  projectId: string,
): Promise<SchemaResponse[]> => {
  const { data } = await apiClient.get<SchemaResponse[]>(
    `/projects/${projectId}/schemas`,
  );
  return data;
};

export const getSchema = async (schemaId: string): Promise<SchemaResponse> => {
  const { data } = await apiClient.get<SchemaResponse>(`/schemas/${schemaId}`);
  return data;
};

export const changeSchemaName = async (
  schemaId: string,
  data: ChangeSchemaNameRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/schemas/${schemaId}/name`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const deleteSchema = async (
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.delete<MutationResponse>(
    `/schemas/${schemaId}`,
    createErdMutationConfig(schemaId),
  );
  return res;
};
