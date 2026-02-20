import { type ApiResponse, bffClient } from '@/lib/api';
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
): Promise<MutationResponse<ConstraintResponse>> => {
  const { data: res } = await bffClient.post<
    ApiResponse<MutationResponse<ConstraintResponse>>
  >('/constraints', data);
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const getConstraint = async (
  constraintId: string,
): Promise<ConstraintResponse> => {
  const { data } = await bffClient.get<ApiResponse<ConstraintResponse>>(
    `/constraints/${constraintId}`,
  );
  if (!data.success || !data.result) {
    throw data.error;
  }
  return data.result;
};

export const getConstraintsByTableId = async (
  tableId: string,
): Promise<ConstraintResponse[]> => {
  const { data } = await bffClient.get<ApiResponse<ConstraintResponse[]>>(
    `/tables/${tableId}/constraints`,
  );
  if (!data.success || !data.result) {
    throw data.error;
  }
  return data.result;
};

export const changeConstraintName = async (
  constraintId: string,
  data: ChangeConstraintNameRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/constraints/${constraintId}/name`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const changeConstraintCheckExpr = async (
  constraintId: string,
  data: ChangeConstraintCheckExprRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/constraints/${constraintId}/check-expr`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const changeConstraintDefaultExpr = async (
  constraintId: string,
  data: ChangeConstraintDefaultExprRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/constraints/${constraintId}/default-expr`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const deleteConstraint = async (
  constraintId: string,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.delete<ApiResponse<MutationResponse>>(
    `/constraints/${constraintId}`,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const getConstraintColumns = async (
  constraintId: string,
): Promise<ConstraintColumnResponse[]> => {
  const { data } = await bffClient.get<ApiResponse<ConstraintColumnResponse[]>>(
    `/constraints/${constraintId}/columns`,
  );
  if (!data.success || !data.result) {
    throw data.error;
  }
  return data.result;
};

export const addConstraintColumn = async (
  constraintId: string,
  data: AddConstraintColumnRequest,
): Promise<MutationResponse<AddConstraintColumnResponse>> => {
  const { data: res } = await bffClient.post<
    ApiResponse<MutationResponse<AddConstraintColumnResponse>>
  >(`/constraints/${constraintId}/columns`, data);
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const removeConstraintColumn = async (
  constraintColumnId: string,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.delete<ApiResponse<MutationResponse>>(
    `/constraint-columns/${constraintColumnId}`,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};

export const getConstraintColumn = async (
  constraintColumnId: string,
): Promise<ConstraintColumnResponse> => {
  const { data } = await bffClient.get<ApiResponse<ConstraintColumnResponse>>(
    `/constraint-columns/${constraintColumnId}`,
  );
  if (!data.success || !data.result) {
    throw data.error;
  }
  return data.result;
};

export const changeConstraintColumnPosition = async (
  constraintColumnId: string,
  data: ChangeConstraintColumnPositionRequest,
): Promise<MutationResponse> => {
  const { data: res } = await bffClient.patch<ApiResponse<MutationResponse>>(
    `/constraint-columns/${constraintColumnId}/position`,
    data,
  );
  if (!res.success || !res.result) {
    throw res.error;
  }
  return res.result;
};
