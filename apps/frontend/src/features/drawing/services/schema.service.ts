import { ulid } from 'ulid';
import type { Schema } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';
import { getSchemaAPI, getSchemaTableListAPI } from '../api/schema.api';
import {
  validateDatabase,
  validateAndGetSchema,
} from '../utils/entityValidators';
import {
  CreateSchemaCommand,
  UpdateSchemaNameCommand,
  DeleteSchemaCommand,
} from '../queue/commands/SchemaCommands';
import { executeCommandWithValidation } from '../utils/commandQueueHelper';

const getErdStore = () => ErdStore.getInstance();

export async function getSchema(schemaId: string) {
  const response = await getSchemaAPI(schemaId);
  return response.result;
}

export async function getSchemaTableList(schemaId: string) {
  const response = await getSchemaTableListAPI(schemaId);
  return response.result;
}

export function createSchema(name: string) {
  const database = validateDatabase();

  if (database.schemas.length === 0) {
    throw new Error('No existing schema to copy defaults from');
  }

  const erdStore = getErdStore();
  const schemaId = ulid();
  const firstSchema = database.schemas[0];

  const newSchema: Schema = {
    id: schemaId,
    name,
    tables: [],
    isAffected: false,
    projectId: firstSchema.projectId,
    dbVendorId: firstSchema.dbVendorId,
    charset: firstSchema.charset,
    collation: firstSchema.collation,
    vendorOption: firstSchema.vendorOption,
  };

  const command = new CreateSchemaCommand(newSchema);

  executeCommandWithValidation(command, () => {
    erdStore.createSchema(newSchema);
  });

  return schemaId;
}

export function updateSchemaName(schemaId: string, newName: string) {
  validateAndGetSchema(schemaId);

  const erdStore = getErdStore();
  const command = new UpdateSchemaNameCommand(schemaId, newName);

  executeCommandWithValidation(command, () => {
    erdStore.changeSchemaName(schemaId, newName);
  });
}

export function deleteSchema(schemaId: string) {
  const { database } = validateAndGetSchema(schemaId);

  if (database.schemas.length <= 1) {
    throw new Error('Cannot delete the last schema');
  }

  const erdStore = getErdStore();
  const command = new DeleteSchemaCommand(schemaId);

  executeCommandWithValidation(command, () => {
    erdStore.deleteSchema(schemaId);
  });
}
