import { useMutation, useQueryClient } from '@tanstack/react-query';
import type {
  AddConstraintColumnRequest,
  ChangeConstraintCheckExprRequest,
  ChangeConstraintColumnPositionRequest,
  ChangeConstraintDefaultExprRequest,
  ChangeConstraintNameRequest,
  CreateConstraintRequest,
  TableSnapshotResponse,
} from '../api';
import {
  addConstraintColumn,
  changeConstraintCheckExpr,
  changeConstraintColumnPosition,
  changeConstraintDefaultExpr,
  changeConstraintName,
  createConstraint,
  deleteConstraint,
  removeConstraintColumn,
} from '../api';
import { useErdCache } from './useErdCache';
import { erdKeys } from './query-keys';
import {
  AddConstraintColumnCommand,
  ChangeConstraintNameCommand,
  CreateConstraintCommand,
  DeleteConstraintCommand,
  RemoveConstraintColumnCommand,
  useErdHistory
} from '../history';

const findConstraintSnapshot = (
  snapshots: Record<string, TableSnapshotResponse> | undefined,
  constraintId: string,
) => {
  for (const snapshot of Object.values(snapshots ?? {})) {
    const cs = snapshot.constraints.find((c) => c.constraint.id === constraintId);
    if (cs) return cs;
  }
  return undefined;
};

export const useCreateConstraint = (schemaId: string) => {
  const {updateAffectedTables} = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateConstraintRequest) => createConstraint(data),
    onSuccess: (result, variables) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new CreateConstraintCommand({
          schemaId,
          queryClient,
          constraintId: result.data.id,
          originalRequest: variables,
        }),
      );
    },
  });
};

export const useChangeConstraintName = (schemaId: string) => {
  const {updateAffectedTables} = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
                   constraintId,
                   data,
                 }: {
      constraintId: string;
      data: ChangeConstraintNameRequest;
    }) => changeConstraintName(constraintId, data),
    onMutate: ({constraintId}) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      const cs = findConstraintSnapshot(snapshots, constraintId);
      return {previousName: cs?.constraint.name ?? ''};
    },
    onSuccess: (result, variables, context) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new ChangeConstraintNameCommand({
          schemaId,
          queryClient,
          constraintId: variables.constraintId,
          previousName: context?.previousName ?? '',
          newName: variables.data.newName,
        }),
      );
    },
  });
};

export const useChangeConstraintCheckExpr = (schemaId: string) => {
  const {updateAffectedTables} = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({constraintId, data,}: {
      constraintId: string;
      data: ChangeConstraintCheckExprRequest;
    }) => changeConstraintCheckExpr(constraintId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeConstraintDefaultExpr = (schemaId: string) => {
  const {updateAffectedTables} = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
                   constraintId,
                   data,
                 }: {
      constraintId: string;
      data: ChangeConstraintDefaultExprRequest;
    }) => changeConstraintDefaultExpr(constraintId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useDeleteConstraint = (schemaId: string) => {
  const {updateAffectedTables} = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (constraintId: string) => deleteConstraint(constraintId),
    onMutate: (constraintId) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      return {snapshot: findConstraintSnapshot(snapshots, constraintId)};
    },
    onSuccess: (result, _constraintId, context) => {
      updateAffectedTables(result.affectedTableIds);
      if (context?.snapshot) {
        history.push(
          new DeleteConstraintCommand({
            schemaId,
            queryClient,
            snapshot: context.snapshot,
          }),
        );
      }
    },
  });
};

export const useAddConstraintColumn = (schemaId: string) => {
  const {updateAffectedTables} = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
                   constraintId,
                   data,
                 }: {
      constraintId: string;
      data: AddConstraintColumnRequest;
    }) => addConstraintColumn(constraintId, data),
    onSuccess: (result, variables) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new AddConstraintColumnCommand({
          schemaId,
          queryClient,
          constraintColumnId: result.data.id,
          constraintId: variables.constraintId,
          columnId: variables.data.columnId,
          seqNo: variables.data.seqNo,
        }),
      );
    },
  });
};

export const useRemoveConstraintColumn = (schemaId: string) => {
  const {updateAffectedTables} = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (constraintColumnId: string) =>
      removeConstraintColumn(constraintColumnId),
    onMutate: (constraintColumnId) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      for (const snapshot of Object.values(snapshots ?? {})) {
        for (const cs of snapshot.constraints) {
          const col = cs.columns.find((c) => c.id === constraintColumnId);
          if (col) {
            return {constraintId: col.constraintId, columnId: col.columnId, seqNo: col.seqNo};
          }
        }
      }
      return {constraintId: '', columnId: '', seqNo: 0};
    },
    onSuccess: (result, _constraintColumnId, context) => {
      updateAffectedTables(result.affectedTableIds);
      if (context?.constraintId) {
        history.push(
          new RemoveConstraintColumnCommand({
            schemaId,
            queryClient,
            constraintId: context.constraintId,
            columnId: context.columnId,
            seqNo: context.seqNo,
          }),
        );
      }
    },
  });
};

export const useChangeConstraintColumnPosition = (schemaId: string) => {
  const {updateAffectedTables} = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
                   constraintColumnId,
                   data,
                 }: {
      constraintColumnId: string;
      data: ChangeConstraintColumnPositionRequest;
    }) => changeConstraintColumnPosition(constraintColumnId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};
