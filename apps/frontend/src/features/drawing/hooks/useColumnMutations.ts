import { useMutation, useQueryClient } from '@tanstack/react-query';
import type {
  ChangeColumnMetaRequest,
  ChangeColumnNameRequest,
  ChangeColumnPositionRequest,
  ChangeColumnTypeRequest,
  CreateColumnRequest,
  TableSnapshotResponse,
} from '../api';
import {
  changeColumnMeta,
  changeColumnName,
  changeColumnPosition,
  changeColumnType,
  createColumn,
  deleteColumn,
} from '../api';
import { useErdCache } from './useErdCache';
import { erdKeys } from './query-keys';
import {
  ChangeColumnNameCommand,
  ChangeColumnTypeCommand,
  CreateColumnCommand,
  DeleteColumnCommand,
  useErdHistory
} from '../history';

const findColumnSnapshot = (
  snapshots: Record<string, TableSnapshotResponse> | undefined,
  columnId: string
) => {
  for (const snapshot of Object.values(snapshots ?? {})) {
    const col = snapshot.columns.find((c) => c.id === columnId);
    if (col) return col;
  }

  return undefined;
}

export const useCreateColumn = (schemaId: string) => {
  const {updateAffectedTables} = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateColumnRequest) => createColumn(data),
    onSuccess: (result, variables) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new CreateColumnCommand({
          schemaId,
          queryClient,
          columnId: result.data.id,
          tableId: variables.tableId,
          originalRequest: variables,
        }),
      );
    },
  });
};

export const useChangeColumnName = (schemaId: string) => {
  const {updateAffectedTables} = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
                   columnId,
                   data,
                 }: {
      columnId: string;
      data: ChangeColumnNameRequest;
    }) => changeColumnName(columnId, data),
    onMutate: ({columnId}) => {
      const snapshots: Record<string, TableSnapshotResponse> | undefined = queryClient.getQueryData(
        erdKeys.schemaSnapshots(schemaId),
      );

      const col = findColumnSnapshot(snapshots, columnId);

      return {previousName: col?.name ?? ''};
    },
    onSuccess: (result, variables, context) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new ChangeColumnNameCommand({
          schemaId,
          queryClient,
          columnId: variables.columnId,
          previousName: context?.previousName ?? '',
          newName: variables.data.newName,
        }),
      );
    },
  });
};

export const useChangeColumnType = (schemaId: string) => {
  const {updateAffectedTables} = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
                   columnId,
                   data,
                 }: {
      columnId: string;
      data: ChangeColumnTypeRequest;
    }) => changeColumnType(columnId, data),
    onMutate: ({columnId}) => {
      const snapshots: Record<string, TableSnapshotResponse> | undefined = queryClient.getQueryData(
        erdKeys.schemaSnapshots(schemaId),
      );

      const col = findColumnSnapshot(snapshots, columnId);

      return {previousType: col?.dataType ?? ''};
    },
    onSuccess: (result, variables, context) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new ChangeColumnTypeCommand({
          schemaId,
          queryClient,
          columnId: variables.columnId,
          previousType: context?.previousType ?? '',
          newType: variables.data.dataType,
        }),
      );
    },
  });
};

export const useChangeColumnMeta = (schemaId: string) => {
  const {updateAffectedTables} = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
                   columnId,
                   data,
                 }: {
      columnId: string;
      data: ChangeColumnMetaRequest;
    }) => changeColumnMeta(columnId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeColumnPosition = (schemaId: string) => {
  const {updateAffectedTables} = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
                   columnId,
                   data,
                 }: {
      columnId: string;
      data: ChangeColumnPositionRequest;
    }) => changeColumnPosition(columnId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useDeleteColumn = (schemaId: string) => {
  const {updateAffectedTables} = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (columnId: string) => deleteColumn(columnId),
    onMutate: (columnId) => {
      const snapshots: Record<string, TableSnapshotResponse> | undefined = queryClient.getQueryData(
        erdKeys.schemaSnapshots(schemaId),
      );

      for (const snapshot of Object.values(snapshots ?? {})) {
        const col = snapshot.columns.find((c) => c.id === columnId);
        if (col) return {tableId: snapshot.table.id, columnData: col};
      }
      return {tableId: '', columnData: undefined};
    },
    onSuccess: (result, columnId, context) => {
      updateAffectedTables(result.affectedTableIds);

      if (context?.columnData) {
        history.push(
          new DeleteColumnCommand({
            schemaId,
            queryClient,
            columnId,
            tableId: context.tableId,
            columnData: context.columnData,
            seqNo: context.columnData.seqNo,
          }),
        );
      }
    },
  });
};
