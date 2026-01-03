import { ulid } from 'ulid';
import type { Table } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';
import {
  createTableAPI,
  getTableAPI,
  getTableColumnListAPI,
  getTableRelationshipListAPI,
  getTableIndexListAPI,
  getTableConstraintListAPI,
  updateTableNameAPI,
  updateTableExtraAPI,
  deleteTableAPI,
} from '../api/table.api';
import { withOptimisticUpdate } from '../utils/optimisticUpdate';
import {
  validateAndGetSchema,
  validateAndGetTable,
} from '../utils/entityValidators';
import { handleServerResponse } from '../utils/sync';

const getErdStore = () => ErdStore.getInstance();

export async function getTable(tableId: string) {
  const response = await getTableAPI(tableId);
  return response.result;
}

export async function getTableColumnList(tableId: string) {
  const response = await getTableColumnListAPI(tableId);
  return response.result;
}

export async function getTableRelationshipList(tableId: string) {
  const response = await getTableRelationshipListAPI(tableId);
  return response.result;
}

export async function getTableIndexList(tableId: string) {
  const response = await getTableIndexListAPI(tableId);
  return response.result;
}

export async function getTableConstraintList(tableId: string) {
  const response = await getTableConstraintListAPI(tableId);
  return response.result;
}

export async function createTable(
  schemaId: string,
  name: string,
  tableOptions: string,
  comment?: string,
  extra?: string,
) {
  const erdStore = getErdStore();
  const { database } = validateAndGetSchema(schemaId);

  const tableId = ulid();

  const newTable: Omit<Table, 'schemaId'> = {
    id: tableId,
    name,
    comment: comment ?? null,
    tableOptions: tableOptions,
    columns: [],
    indexes: [],
    constraints: [],
    relationships: [],
    isAffected: false,
    extra: extra ? JSON.parse(extra) : undefined,
  };

  const response = await withOptimisticUpdate(
    () => erdStore.createTable(schemaId, newTable),
    () =>
      createTableAPI(
        {
          database,
          schemaId,
          table: {
            id: tableId,
            schemaId,
            name,
            comment: comment ?? '',
            tableOptions: tableOptions ?? '',
          },
        },
        extra,
      ),
    () => erdStore.deleteTable(schemaId, tableId),
  );

  handleServerResponse(response, { schemaId, tableId });

  return tableId;
}

export async function updateTableName(
  schemaId: string,
  tableId: string,
  newName: string,
) {
  const erdStore = getErdStore();
  const { database, table } = validateAndGetTable(schemaId, tableId);

  await withOptimisticUpdate(
    () => {
      const oldName = table.name;
      erdStore.changeTableName(schemaId, tableId, newName);
      return oldName;
    },
    () =>
      updateTableNameAPI(tableId, {
        database,
        schemaId,
        tableId,
        newName,
      }),
    (oldName) => erdStore.changeTableName(schemaId, tableId, oldName),
  );
}

export async function updateTableExtra(
  schemaId: string,
  tableId: string,
  extra: unknown,
) {
  const erdStore = getErdStore();
  const { table } = validateAndGetTable(schemaId, tableId);

  await withOptimisticUpdate(
    () => {
      const oldExtra = table.extra;
      erdStore.updateTableExtra(schemaId, tableId, extra);
      return oldExtra;
    },
    () =>
      updateTableExtraAPI(tableId, {
        extra: JSON.stringify(extra),
      }),
    (oldExtra) => erdStore.updateTableExtra(schemaId, tableId, oldExtra),
  );
}

export async function deleteTable(schemaId: string, tableId: string) {
  const erdStore = getErdStore();
  const { database, table } = validateAndGetTable(schemaId, tableId);

  await withOptimisticUpdate(
    () => {
      const tableSnapshot = structuredClone(table);
      erdStore.deleteTable(schemaId, tableId);
      return tableSnapshot;
    },
    () =>
      deleteTableAPI(tableId, {
        database,
        schemaId,
        tableId,
      }),
    (tableSnapshot) => erdStore.createTable(schemaId, tableSnapshot),
  );
}
