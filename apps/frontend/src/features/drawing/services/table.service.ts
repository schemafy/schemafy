import { ulid } from 'ulid';
import type { Table } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';
import {
  getTableAPI,
  getTableColumnListAPI,
  getTableRelationshipListAPI,
  getTableIndexListAPI,
  getTableConstraintListAPI,
} from '../api/table.api';
import {
  validateAndGetSchema,
  validateAndGetTable,
} from '../utils/entityValidators';
import {
  CreateTableCommand,
  UpdateTableNameCommand,
  UpdateTableExtraCommand,
  DeleteTableCommand,
} from '../queue/commands/TableCommands';
import { executeCommandWithValidation } from '../utils/commandQueueHelper';

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

export function createTable(
  schemaId: string,
  name: string,
  tableOptions: string,
  comment?: string,
  extra?: string,
) {
  validateAndGetSchema(schemaId);

  const erdStore = getErdStore();
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

  const command = new CreateTableCommand(schemaId, newTable);

  executeCommandWithValidation(command, () => {
    erdStore.createTable(schemaId, newTable);
  });

  return tableId;
}

export function updateTableName(
  schemaId: string,
  tableId: string,
  newName: string,
) {
  validateAndGetTable(schemaId, tableId);

  const erdStore = getErdStore();
  const command = new UpdateTableNameCommand(schemaId, tableId, newName);

  executeCommandWithValidation(command, () => {
    erdStore.changeTableName(schemaId, tableId, newName);
  });
}

export function updateTableExtra(
  schemaId: string,
  tableId: string,
  extra: unknown,
) {
  validateAndGetTable(schemaId, tableId);

  const erdStore = getErdStore();
  const command = new UpdateTableExtraCommand(schemaId, tableId, extra);

  executeCommandWithValidation(command, () => {
    erdStore.updateTableExtra(schemaId, tableId, extra);
  });
}

export function deleteTable(schemaId: string, tableId: string) {
  const { table } = validateAndGetTable(schemaId, tableId);

  const erdStore = getErdStore();
  const tableSnapshot = structuredClone(table);
  const command = new DeleteTableCommand(schemaId, tableId, tableSnapshot);

  executeCommandWithValidation(command, () => {
    erdStore.deleteTable(schemaId, tableId);
  });
}
