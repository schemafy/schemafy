import { useMutation } from '@tanstack/react-query';
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
  CreateIndexRequest,
  ChangeIndexNameRequest,
  ChangeIndexTypeRequest,
  AddIndexColumnRequest,
  ChangeIndexColumnPositionRequest,
  ChangeIndexColumnSortDirectionRequest,
} from '../api';
import { useErdCache } from './useErdCache';

export const useCreateIndex = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (data: CreateIndexRequest) => createIndex(data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeIndexName = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      indexId,
      data,
    }: {
      indexId: string;
      data: ChangeIndexNameRequest;
    }) => changeIndexName(indexId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeIndexType = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      indexId,
      data,
    }: {
      indexId: string;
      data: ChangeIndexTypeRequest;
    }) => changeIndexType(indexId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useDeleteIndex = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (indexId: string) => deleteIndex(indexId, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useAddIndexColumn = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      indexId,
      data,
    }: {
      indexId: string;
      data: AddIndexColumnRequest;
    }) => addIndexColumn(indexId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useRemoveIndexColumn = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (indexColumnId: string) =>
      removeIndexColumn(indexColumnId, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeIndexColumnPosition = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      indexColumnId,
      data,
    }: {
      indexColumnId: string;
      data: ChangeIndexColumnPositionRequest;
    }) => changeIndexColumnPosition(indexColumnId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeIndexColumnSortDirection = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      indexColumnId,
      data,
    }: {
      indexColumnId: string;
      data: ChangeIndexColumnSortDirectionRequest;
    }) => changeIndexColumnSortDirection(indexColumnId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};
