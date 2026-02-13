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
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (data: CreateIndexRequest) => createIndex(data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeIndexName = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      indexId,
      data,
    }: {
      indexId: string;
      data: ChangeIndexNameRequest;
    }) => changeIndexName(indexId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeIndexType = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      indexId,
      data,
    }: {
      indexId: string;
      data: ChangeIndexTypeRequest;
    }) => changeIndexType(indexId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useDeleteIndex = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (indexId: string) => deleteIndex(indexId),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useAddIndexColumn = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      indexId,
      data,
    }: {
      indexId: string;
      data: AddIndexColumnRequest;
    }) => addIndexColumn(indexId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useRemoveIndexColumn = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (indexColumnId: string) => removeIndexColumn(indexColumnId),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
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
  return useMutation({
    mutationFn: ({
      indexColumnId,
      data,
    }: {
      indexColumnId: string;
      data: ChangeIndexColumnSortDirectionRequest;
    }) => changeIndexColumnSortDirection(indexColumnId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};
