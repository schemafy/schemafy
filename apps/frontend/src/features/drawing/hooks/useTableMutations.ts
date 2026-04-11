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
import { syncCommittedRevision } from '../api/mutation-request';
import { useErdCache } from './useErdCache';

export const useCreateTableWithExtra = (schemaId: string) => {
  const { updateAffectedTables } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (data: {
      request: CreateTableRequest;
      extra: NonNullable<CreateTableRequest['extra']>;
    }) => createTable({ ...data.request, extra: data.extra }),
    onSuccess: (result) => {
      syncCommittedRevision(schemaId, result);
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
    }) => changeTableName(tableId, data, schemaId),
    onSuccess: (result) => {
      syncCommittedRevision(schemaId, result);
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
    }) => changeTableMeta(tableId, data, schemaId),
    onSuccess: (result) => {
      syncCommittedRevision(schemaId, result);
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
    }) => changeTableExtra(tableId, data, schemaId),
    onSuccess: (result) => {
      syncCommittedRevision(schemaId, result);
      updateAffectedTables(result.affectedTableIds);
    },
  });
};

export const useDeleteTable = (schemaId: string) => {
  const { removeAndUpdate } = useErdCache(schemaId);
  return useMutation({
    mutationFn: (tableId: string) => deleteTable(tableId, schemaId),
    onSuccess: (result, tableId) => {
      syncCommittedRevision(schemaId, result);
      removeAndUpdate(tableId, result.affectedTableIds);
    },
  });
};
