import { useMutation } from '@tanstack/react-query';
import {
  createColumn,
  changeColumnName,
  changeColumnType,
  changeColumnMeta,
  changeColumnPosition,
  deleteColumn,
} from '../api';
import type {
  CreateColumnRequest,
  ChangeColumnNameRequest,
  ChangeColumnTypeRequest,
  ChangeColumnMetaRequest,
  ChangeColumnPositionRequest,
} from '../api';
import { useErdCache } from './useErdCache';

export const useCreateColumn = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (data: CreateColumnRequest) => createColumn(data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeColumnName = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      columnId,
      data,
    }: {
      columnId: string;
      data: ChangeColumnNameRequest;
    }) => changeColumnName(columnId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeColumnType = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      columnId,
      data,
    }: {
      columnId: string;
      data: ChangeColumnTypeRequest;
    }) => changeColumnType(columnId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeColumnMeta = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      columnId,
      data,
    }: {
      columnId: string;
      data: ChangeColumnMetaRequest;
    }) => changeColumnMeta(columnId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeColumnPosition = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      columnId,
      data,
    }: {
      columnId: string;
      data: ChangeColumnPositionRequest;
    }) => changeColumnPosition(columnId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useDeleteColumn = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (columnId: string) => deleteColumn(columnId, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};
