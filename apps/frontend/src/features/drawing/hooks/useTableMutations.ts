import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createTable,
  changeTableName,
  changeTableMeta,
  changeTableExtra,
  deleteTable,
} from '../api';
import type {
  TableSnapshotResponse,
  CreateTableRequest,
  ChangeTableNameRequest,
  ChangeTableMetaRequest,
  ChangeTableExtraRequest,
} from '../api';
import { useErdCache } from './useErdCache';
import { erdKeys } from './query-keys';
import { useErdHistory } from '../history';
import {
  CreateTableCommand,
  DeleteTableCommand,
  ChangeTableNameCommand,
  MoveTableCommand,
} from '../history';
import { collectCascadeSnapshots } from '../history/cascadeHelpers';

export const useCreateTableWithExtra = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { request: CreateTableRequest; extra: string }) =>
      createTable({ ...data.request, extra: data.extra }),
    onSuccess: (result, variables) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new CreateTableCommand({
          schemaId,
          queryClient,
          tableId: result.data.id,
          originalRequest: variables.request,
          originalExtra: variables.extra,
        }),
      );
    },
  });
};

export const useChangeTableName = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      tableId,
      data,
    }: {
      tableId: string;
      data: ChangeTableNameRequest;
    }) => changeTableName(tableId, data),
    onMutate: ({ tableId }) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      return { previousName: snapshots?.[tableId]?.table.name ?? '' };
    },
    onSuccess: (result, variables, context) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new ChangeTableNameCommand({
          schemaId,
          queryClient,
          tableId: variables.tableId,
          previousName: context?.previousName ?? '',
          newName: variables.data.newName,
        }),
      );
    },
  });
};

export const useChangeTableMeta = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      tableId,
      data,
    }: {
      tableId: string;
      data: ChangeTableMetaRequest;
    }) => changeTableMeta(tableId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeTableExtra = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      tableId,
      data,
    }: {
      tableId: string;
      data: ChangeTableExtraRequest;
    }) => changeTableExtra(tableId, data),
    onMutate: ({ tableId }) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      return { previousExtra: snapshots?.[tableId]?.table.extra ?? '{}' };
    },
    onSuccess: (result, variables, context) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new MoveTableCommand({
          schemaId,
          queryClient,
          tableId: variables.tableId,
          previousExtra: context?.previousExtra ?? '{}',
          newExtra: variables.data.extra,
        }),
      );
    },
  });
};

export const useDeleteTable = (schemaId: string) => {
  const { removeAndUpdate } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (tableId: string) => deleteTable(tableId),
    onMutate: (tableId) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      const cascadeSnapshots: Record<string, TableSnapshotResponse> = {};
      if (snapshots) {
        collectCascadeSnapshots(tableId, snapshots, cascadeSnapshots);
      }
      return { snapshot: snapshots?.[tableId], cascadeSnapshots };
    },
    onSuccess: (result, tableId, context) => {
      removeAndUpdate(tableId, result.affectedTableIds);
      if (context?.snapshot) {
        history.push(
          new DeleteTableCommand({
            schemaId,
            queryClient,
            tableId,
            snapshot: context.snapshot,
            cascadeSnapshots: context.cascadeSnapshots ?? {},
          }),
        );
      }
    },
  });
};