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
  const database = erdStore.database;

  if (!database) {
    throw new Error('Database not loaded');
  }

  const schema = database.schemas.find((s) => s.id === schemaId);
  if (!schema) {
    throw new Error(`Schema ${schemaId} not found`);
  }

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

  const realId = response.result?.tables[tableId];
  if (realId && realId !== tableId) {
    erdStore.replaceTableId(schemaId, tableId, realId);
    return realId;
  }

  return tableId;
}

export async function updateTableName(
  schemaId: string,
  tableId: string,
  newName: string,
) {
  const erdStore = getErdStore();
  const database = erdStore.database;

  if (!database) {
    throw new Error('Database not loaded');
  }

  const schema = database.schemas.find((s) => s.id === schemaId);
  if (!schema) {
    throw new Error(`Schema ${schemaId} not found`);
  }

  const table = schema.tables.find((t) => t.id === tableId);
  if (!table) {
    throw new Error(`Table ${tableId} not found`);
  }

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

  const schema = erdStore.database?.schemas.find((s) => s.id === schemaId);
  if (!schema) {
    throw new Error(`Schema ${schemaId} not found`);
  }

  const table = schema.tables.find((t) => t.id === tableId);
  if (!table) {
    throw new Error(`Table ${tableId} not found`);
  }

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
  const database = erdStore.database;

  if (!database) {
    throw new Error('Database not loaded');
  }

  const schema = database.schemas.find((s) => s.id === schemaId);
  if (!schema) {
    throw new Error(`Schema ${schemaId} not found`);
  }

  const table = schema.tables.find((t) => t.id === tableId);
  if (!table) {
    throw new Error(`Table ${tableId} not found`);
  }

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
