import { useMutation } from '@tanstack/react-query';
import {
  createTable,
  changeTableName,
  changeTableMeta,
  changeTableExtra,
  deleteTable,
} from '../api';
import type {
  CreateTableRequest,
  ChangeTableNameRequest,
  ChangeTableMetaRequest,
  ChangeTableExtraRequest,
} from '../api';
import { useErdCache } from './useErdCache';

export const useCreateTable = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (data: CreateTableRequest) => createTable(data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useChangeTableName = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      tableId,
      data,
    }: {
      tableId: string;
      data: ChangeTableNameRequest;
    }) => changeTableName(tableId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
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
  return useMutation({
    mutationFn: ({
      tableId,
      data,
    }: {
      tableId: string;
      data: ChangeTableExtraRequest;
    }) => changeTableExtra(tableId, data),
    onSuccess: (result) => {
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useDeleteTable = (schemaId: string) => {
  const { removeAndUpdate } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (tableId: string) => deleteTable(tableId),
    onSuccess: (result, tableId) => {
      removeAndUpdate(tableId, result.affectedTableIds);
    },
  });
};
