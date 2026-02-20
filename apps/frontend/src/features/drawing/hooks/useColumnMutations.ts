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
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (data: CreateColumnRequest) => createColumn(data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeColumnName = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      columnId,
      data,
    }: {
      columnId: string;
      data: ChangeColumnNameRequest;
    }) => changeColumnName(columnId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeColumnType = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      columnId,
      data,
    }: {
      columnId: string;
      data: ChangeColumnTypeRequest;
    }) => changeColumnType(columnId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeColumnMeta = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
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
  const { updateAffectedTables } = useErdCache(schemaId);
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
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (columnId: string) => deleteColumn(columnId),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};
