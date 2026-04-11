import { apiClient } from '@/lib/api';
import { createErdMutationConfig } from './mutation-request';
import type {
  MutationResponse,
  ConstraintResponse,
  CreateConstraintRequest,
  ChangeConstraintNameRequest,
  ChangeConstraintCheckExprRequest,
  ChangeConstraintDefaultExprRequest,
  ConstraintColumnResponse,
  AddConstraintColumnRequest,
  AddConstraintColumnResponse,
  ChangeConstraintColumnPositionRequest,
} from './types';

export const createConstraint = async (
  data: CreateConstraintRequest,
  schemaId: string,
): Promise<MutationResponse<ConstraintResponse>> => {
  const { data: res } = await apiClient.post<
    MutationResponse<ConstraintResponse>
  >('/constraints', data, createErdMutationConfig(schemaId));
  return res;
};

export const getConstraint = async (
  constraintId: string,
): Promise<ConstraintResponse> => {
  const { data } = await apiClient.get<ConstraintResponse>(
    `/constraints/${constraintId}`,
  );
  return data;
};

export const getConstraintsByTableId = async (
  tableId: string,
): Promise<ConstraintResponse[]> => {
  const { data } = await apiClient.get<ConstraintResponse[]>(
    `/tables/${tableId}/constraints`,
  );
  return data;
};

export const changeConstraintName = async (
  constraintId: string,
  data: ChangeConstraintNameRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/constraints/${constraintId}/name`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const changeConstraintCheckExpr = async (
  constraintId: string,
  data: ChangeConstraintCheckExprRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/constraints/${constraintId}/check-expr`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const changeConstraintDefaultExpr = async (
  constraintId: string,
  data: ChangeConstraintDefaultExprRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/constraints/${constraintId}/default-expr`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const deleteConstraint = async (
  constraintId: string,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.delete<MutationResponse>(
    `/constraints/${constraintId}`,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const getConstraintColumns = async (
  constraintId: string,
): Promise<ConstraintColumnResponse[]> => {
  const { data } = await apiClient.get<ConstraintColumnResponse[]>(
    `/constraints/${constraintId}/columns`,
  );
  return data;
};

export const addConstraintColumn = async (
  constraintId: string,
  data: AddConstraintColumnRequest,
  schemaId: string,
): Promise<MutationResponse<AddConstraintColumnResponse>> => {
  const { data: res } = await apiClient.post<
    MutationResponse<AddConstraintColumnResponse>
  >(
    `/constraints/${constraintId}/columns`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const removeConstraintColumn = async (
  constraintColumnId: string,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.delete<MutationResponse>(
    `/constraint-columns/${constraintColumnId}`,
    createErdMutationConfig(schemaId),
  );
  return res;
};

export const getConstraintColumn = async (
  constraintColumnId: string,
): Promise<ConstraintColumnResponse> => {
  const { data } = await apiClient.get<ConstraintColumnResponse>(
    `/constraint-columns/${constraintColumnId}`,
  );
  return data;
};

export const changeConstraintColumnPosition = async (
  constraintColumnId: string,
  data: ChangeConstraintColumnPositionRequest,
  schemaId: string,
): Promise<MutationResponse> => {
  const { data: res } = await apiClient.patch<MutationResponse>(
    `/constraint-columns/${constraintColumnId}/position`,
    data,
    createErdMutationConfig(schemaId),
  );
  return res;
};
