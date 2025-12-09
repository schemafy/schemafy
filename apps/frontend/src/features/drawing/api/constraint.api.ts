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

export const createConstraint = (data: CreateConstraintRequest) =>
  api.post<AffectedMappingResponse>('/api/constraints', data);

export const getConstraint = (constraintId: string) =>
  api.get<ConstraintResponse>(`/api/constraints/${constraintId}`);

export const updateConstraintName = (
  constraintId: string,
  data: UpdateConstraintNameRequest,
) => api.put<ConstraintResponse>(`/api/constraints/${constraintId}/name`, data);

export const addColumnToConstraint = (
  constraintId: string,
  data: AddColumnToConstraintRequest,
) =>
  api.post<AffectedMappingResponse>(
    `/api/constraints/${constraintId}/columns`,
    data,
  );

export const removeColumnFromConstraint = (
  constraintId: string,
  columnId: string,
  data: RemoveColumnFromConstraintRequest,
) =>
  api.delete<null>(`/api/constraints/${constraintId}/columns/${columnId}`, {
    data,
  });

export const deleteConstraint = (
  constraintId: string,
  data: DeleteConstraintRequest,
) => api.delete<null>(`/api/constraints/${constraintId}`, { data });
