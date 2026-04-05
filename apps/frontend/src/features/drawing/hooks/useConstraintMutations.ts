import { useMutation } from '@tanstack/react-query';
import {
  createConstraint,
  changeConstraintName,
  changeConstraintCheckExpr,
  changeConstraintDefaultExpr,
  deleteConstraint,
  addConstraintColumn,
  removeConstraintColumn,
  changeConstraintColumnPosition,
} from '../api';
import type {
  CreateConstraintRequest,
  ChangeConstraintNameRequest,
  ChangeConstraintCheckExprRequest,
  ChangeConstraintDefaultExprRequest,
  AddConstraintColumnRequest,
  ChangeConstraintColumnPositionRequest,
} from '../api';
import { syncCommittedRevision } from '../api/mutation-request';
import { useErdCache } from './useErdCache';

export const useCreateConstraint = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (data: CreateConstraintRequest) =>
      createConstraint(data, schemaId),
    onSuccess: (result) => {
      syncCommittedRevision(schemaId, result);
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeConstraintName = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      constraintId,
      data,
    }: {
      constraintId: string;
      data: ChangeConstraintNameRequest;
    }) => changeConstraintName(constraintId, data, schemaId),
    onSuccess: (result) => {
      syncCommittedRevision(schemaId, result);
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeConstraintCheckExpr = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      constraintId,
      data,
    }: {
      constraintId: string;
      data: ChangeConstraintCheckExprRequest;
    }) => changeConstraintCheckExpr(constraintId, data, schemaId),
    onSuccess: (result) => {
      syncCommittedRevision(schemaId, result);
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeConstraintDefaultExpr = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      constraintId,
      data,
    }: {
      constraintId: string;
      data: ChangeConstraintDefaultExprRequest;
    }) => changeConstraintDefaultExpr(constraintId, data, schemaId),
    onSuccess: (result) => {
      syncCommittedRevision(schemaId, result);
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useDeleteConstraint = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (constraintId: string) =>
      deleteConstraint(constraintId, schemaId),
    onSuccess: (result) => {
      syncCommittedRevision(schemaId, result);
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useAddConstraintColumn = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      constraintId,
      data,
    }: {
      constraintId: string;
      data: AddConstraintColumnRequest;
    }) => addConstraintColumn(constraintId, data, schemaId),
    onSuccess: (result) => {
      syncCommittedRevision(schemaId, result);
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useRemoveConstraintColumn = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (constraintColumnId: string) =>
      removeConstraintColumn(constraintColumnId, schemaId),
    onSuccess: (result) => {
      syncCommittedRevision(schemaId, result);
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeConstraintColumnPosition = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      constraintColumnId,
      data,
    }: {
      constraintColumnId: string;
      data: ChangeConstraintColumnPositionRequest;
    }) => changeConstraintColumnPosition(constraintColumnId, data, schemaId),
    onSuccess: (result) => {
      syncCommittedRevision(schemaId, result);
      updateAffectedTables(result.affectedTableIds);
    },
  });
};
