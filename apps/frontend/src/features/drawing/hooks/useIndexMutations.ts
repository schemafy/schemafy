import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createIndex,
  changeIndexName,
  changeIndexType,
  deleteIndex,
  addIndexColumn,
  removeIndexColumn,
  changeIndexColumnPosition,
  changeIndexColumnSortDirection,
} from '../api';
import type {
  TableSnapshotResponse,
  CreateIndexRequest,
  ChangeIndexNameRequest,
  ChangeIndexTypeRequest,
  AddIndexColumnRequest,
  ChangeIndexColumnPositionRequest,
  ChangeIndexColumnSortDirectionRequest,
} from '../api';
import { useErdCache } from './useErdCache';
import { erdKeys } from './query-keys';
import { useErdHistory } from '../history';
import {
  CreateIndexCommand,
  DeleteIndexCommand,
  ChangeIndexNameCommand,
  ChangeIndexTypeCommand,
  AddIndexColumnCommand,
  RemoveIndexColumnCommand,
  ChangeIndexColumnSortDirectionCommand,
} from '../history';

const findIndexSnapshot = (
  snapshots: Record<string, TableSnapshotResponse> | undefined,
  indexId: string,
) => {
  for (const snapshot of Object.values(snapshots ?? {})) {
    const idx = snapshot.indexes.find((i) => i.index.id === indexId);
    if (idx) return idx;
  }
  return undefined;
};

export const useCreateIndex = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateIndexRequest) => createIndex(data),
    onSuccess: (result, variables) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new CreateIndexCommand({
          schemaId,
          queryClient,
          indexId: result.data.id,
          originalRequest: variables,
        }),
      );
    },
  });
};

export const useChangeIndexName = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      indexId,
      data,
    }: {
      indexId: string;
      data: ChangeIndexNameRequest;
    }) => changeIndexName(indexId, data),
    onMutate: ({ indexId }) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      const idx = findIndexSnapshot(snapshots, indexId);
      return { previousName: idx?.index.name ?? '' };
    },
    onSuccess: (result, variables, context) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new ChangeIndexNameCommand({
          schemaId,
          queryClient,
          indexId: variables.indexId,
          previousName: context?.previousName ?? '',
          newName: variables.data.newName,
        }),
      );
    },
  });
};

export const useChangeIndexType = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      indexId,
      data,
    }: {
      indexId: string;
      data: ChangeIndexTypeRequest;
    }) => changeIndexType(indexId, data),
    onMutate: ({ indexId }) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      const idx = findIndexSnapshot(snapshots, indexId);
      return { previousType: idx?.index.type ?? '' };
    },
    onSuccess: (result, variables, context) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new ChangeIndexTypeCommand({
          schemaId,
          queryClient,
          indexId: variables.indexId,
          previousType: context?.previousType ?? '',
          newType: variables.data.type,
        }),
      );
    },
  });
};

export const useDeleteIndex = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (indexId: string) => deleteIndex(indexId),
    onMutate: (indexId) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      return { snapshot: findIndexSnapshot(snapshots, indexId) };
    },
    onSuccess: (result, _indexId, context) => {
      updateAffectedTables(result.affectedTableIds);
      if (context?.snapshot) {
        history.push(
          new DeleteIndexCommand({
            schemaId,
            queryClient,
            snapshot: context.snapshot,
          }),
        );
      }
    },
  });
};

export const useAddIndexColumn = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      indexId,
      data,
    }: {
      indexId: string;
      data: AddIndexColumnRequest;
    }) => addIndexColumn(indexId, data),
    onSuccess: (result, variables) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new AddIndexColumnCommand({
          schemaId,
          queryClient,
          indexColumnId: result.data.id,
          indexId: variables.indexId,
          columnId: variables.data.columnId,
          seqNo: variables.data.seqNo,
          sortDirection: variables.data.sortDirection,
        }),
      );
    },
  });
};

export const useRemoveIndexColumn = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (indexColumnId: string) => removeIndexColumn(indexColumnId),
    onMutate: (indexColumnId) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      for (const snapshot of Object.values(snapshots ?? {})) {
        for (const idx of snapshot.indexes) {
          const col = idx.columns.find((c) => c.id === indexColumnId);
          if (col) {
            return {
              indexId: col.indexId,
              columnId: col.columnId,
              seqNo: col.seqNo,
              sortDirection: col.sortDirection,
            };
          }
        }
      }
      return { indexId: '', columnId: '', seqNo: 0, sortDirection: 'ASC' };
    },
    onSuccess: (result, _indexColumnId, context) => {
      updateAffectedTables(result.affectedTableIds);
      if (context?.indexId) {
        history.push(
          new RemoveIndexColumnCommand({
            schemaId,
            queryClient,
            indexId: context.indexId,
            columnId: context.columnId,
            seqNo: context.seqNo,
            sortDirection: context.sortDirection,
          }),
        );
      }
    },
  });
};

export const useChangeIndexColumnPosition = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      indexColumnId,
      data,
    }: {
      indexColumnId: string;
      data: ChangeIndexColumnPositionRequest;
    }) => changeIndexColumnPosition(indexColumnId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeIndexColumnSortDirection = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  const history = useErdHistory();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      indexColumnId,
      data,
    }: {
      indexColumnId: string;
      data: ChangeIndexColumnSortDirectionRequest;
    }) => changeIndexColumnSortDirection(indexColumnId, data),
    onMutate: ({ indexColumnId }) => {
      const snapshots = queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );
      for (const snapshot of Object.values(snapshots ?? {})) {
        for (const idx of snapshot.indexes) {
          const col = idx.columns.find((c) => c.id === indexColumnId);
          if (col) return { previousSortDirection: col.sortDirection };
        }
      }
      return { previousSortDirection: '' };
    },
    onSuccess: (result, variables, context) => {
      updateAffectedTables(result.affectedTableIds);
      history.push(
        new ChangeIndexColumnSortDirectionCommand({
          schemaId,
          queryClient,
          indexColumnId: variables.indexColumnId,
          previousSortDirection: context?.previousSortDirection ?? '',
          newSortDirection: variables.data.sortDirection,
        }),
      );
    },
  });
};
