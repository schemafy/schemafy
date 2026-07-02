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

export const useCreateTableWithExtra = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (data: {
      request: CreateTableRequest;
      extra: NonNullable<CreateTableRequest['extra']>;
    }) => createTable({ ...data.request, extra: data.extra }),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeTableName = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      tableId,
      data,
    }: {
      tableId: string;
      data: ChangeTableNameRequest;
    }) => changeTableName(tableId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeTableMeta = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      tableId,
      data,
    }: {
      tableId: string;
      data: ChangeTableMetaRequest;
    }) => changeTableMeta(tableId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useChangeTableExtra = (schemaId: string) => {
  const { syncAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: ({
      tableId,
      data,
    }: {
      tableId: string;
      data: ChangeTableExtraRequest;
    }) => changeTableExtra(tableId, data, schemaId),
    onSuccess: (result) => {
      syncAffectedTables(result);
    },
  });
};

export const useDeleteTable = (schemaId: string) => {
  const { syncRemovedTable } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (tableId: string) => deleteTable(tableId, schemaId),
    onSuccess: (result, tableId) => {
      syncRemovedTable(tableId, result);
    },
  });
};
