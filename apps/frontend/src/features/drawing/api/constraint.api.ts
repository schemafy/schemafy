import { api } from '@/lib/api/helpers';
import type {
  ConstraintResponse,
  CreateConstraintRequest,
  UpdateConstraintNameRequest,
  AddColumnToConstraintRequest,
  RemoveColumnFromConstraintRequest,
  DeleteConstraintRequest,
} from './types/constraint';
import type { AffectedMappingResponse } from './types/common';

export const createConstraintAPI = (data: CreateConstraintRequest) =>
  api.post<AffectedMappingResponse>('/constraints', data);

export const getConstraintAPI = (constraintId: string) =>
  api.get<ConstraintResponse>(`/constraints/${constraintId}`);

export const updateConstraintNameAPI = (
  constraintId: string,
  data: UpdateConstraintNameRequest,
) => api.put<ConstraintResponse>(`/constraints/${constraintId}/name`, data);

export const addColumnToConstraintAPI = (
  constraintId: string,
  data: AddColumnToConstraintRequest,
) =>
  api.post<AffectedMappingResponse>(
    `/constraints/${constraintId}/columns`,
    data,
  );

export const removeColumnFromConstraintAPI = (
  constraintId: string,
  columnId: string,
  data: RemoveColumnFromConstraintRequest,
) =>
  api.delete<null>(`/constraints/${constraintId}/columns/${columnId}`, {
    data,
  });

export const deleteConstraintAPI = (
  constraintId: string,
  data: DeleteConstraintRequest,
) => api.delete<null>(`/constraints/${constraintId}`, { data });
