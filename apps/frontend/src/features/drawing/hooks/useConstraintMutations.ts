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
import { useErdCache } from './useErdCache';

export const useCreateConstraint = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (data: CreateConstraintRequest) =>
      createConstraint(data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeConstraintName = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      constraintId,
      data,
    }: {
      constraintId: string;
      data: ChangeConstraintNameRequest;
    }) => changeConstraintName(constraintId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeConstraintCheckExpr = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      constraintId,
      data,
    }: {
      constraintId: string;
      data: ChangeConstraintCheckExprRequest;
    }) => changeConstraintCheckExpr(constraintId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeConstraintDefaultExpr = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      constraintId,
      data,
    }: {
      constraintId: string;
      data: ChangeConstraintDefaultExprRequest;
    }) => changeConstraintDefaultExpr(constraintId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useDeleteConstraint = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (constraintId: string) =>
      deleteConstraint(constraintId, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useAddConstraintColumn = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      constraintId,
      data,
    }: {
      constraintId: string;
      data: AddConstraintColumnRequest;
    }) => addConstraintColumn(constraintId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useRemoveConstraintColumn = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (constraintColumnId: string) =>
      removeConstraintColumn(constraintColumnId, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeConstraintColumnPosition = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      constraintColumnId,
      data,
    }: {
      constraintColumnId: string;
      data: ChangeConstraintColumnPositionRequest;
    }) => changeConstraintColumnPosition(constraintColumnId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};
