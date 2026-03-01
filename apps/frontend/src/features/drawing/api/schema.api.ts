import { type ApiResponse, apiClient } from '@/lib/api';
import type {
  MutationResponse,
  SchemaResponse,
  CreateSchemaRequest,
  ChangeSchemaNameRequest,
} from './types';

export const createSchema = async (
  data: CreateSchemaRequest,
): Promise<MutationResponse<SchemaResponse>> => {
  const { data: res } = await apiClient.post<
    ApiResponse<MutationResponse<SchemaResponse>>
  >('/schemas', data);
  if (!res.success || !res.result) {
    throw res.error ?? new Error('Unknown API error');
  }
  return res.result;
};

export const getSchemasByProjectId = async (
  projectId: string,
): Promise<SchemaResponse[]> => {
  const { data } = await apiClient.get<ApiResponse<SchemaResponse[]>>(
    `/projects/${projectId}/schemas`,
  );
  if (!data.success || !data.result) {
    throw data.error ?? new Error('Unknown API error');
  }
  return data.result;
};

export const getSchema = async (schemaId: string): Promise<SchemaResponse> => {
  const { data } = await apiClient.get<ApiResponse<SchemaResponse>>(
    `/schemas/${schemaId}`,
  );
  if (!data.success || !data.result) {
    throw data.error ?? new Error('Unknown API error');
  }
  return data.result;
};

export const changeSchemaName = async (
  schemaId: string,
  data: ChangeSchemaNameRequest,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<ApiResponse<MutationResponse>>(
    `/schemas/${schemaId}/name`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error ?? new Error('Unknown API error');
  }
  return res.result;
};

export const deleteSchema = async (
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.delete<ApiResponse<MutationResponse>>(
    `/schemas/${schemaId}`,
  );
  if (!res.success || !res.result) {
    throw res.error ?? new Error('Unknown API error');
  }
  return res.result;
};
