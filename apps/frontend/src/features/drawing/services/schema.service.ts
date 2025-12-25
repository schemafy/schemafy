import { ulid } from 'ulid';
import type { Schema } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';
import {
  createSchemaAPI,
  getSchemaAPI,
  getSchemaTableListAPI,
  updateSchemaNameAPI,
  deleteSchemaAPI,
} from '../api/schema.api';
import { withOptimisticUpdate } from '../utils/optimisticUpdate';
import {
  validateDatabase,
  validateAndGetSchema,
} from '../utils/entityValidators';
import { handleSchemaIdRemapping } from '../utils/idRemapping';

const getErdStore = () => ErdStore.getInstance();

export async function getSchema(schemaId: string) {
  const response = await getSchemaAPI(schemaId);
  return response.result;
}

export async function getSchemaTableList(schemaId: string) {
  const response = await getSchemaTableListAPI(schemaId);
  return response.result;
}

export async function createSchema(name: string) {
  const erdStore = getErdStore();
  const database = validateDatabase();

  if (database.schemas.length === 0) {
    throw new Error('No existing schema to copy defaults from');
  }

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

  const response = await withOptimisticUpdate(
    () => erdStore.createSchema(newSchema),
    () =>
      createSchemaAPI({
        database,
        schema: {
          id: schemaId,
          projectId: firstSchema.projectId,
          dbVendorId: firstSchema.dbVendorId,
          name,
          charset: firstSchema.charset ?? '',
          collation: firstSchema.collation ?? '',
          vendorOption: firstSchema.vendorOption ?? '',
        },
      }),
    () => erdStore.deleteSchema(schemaId),
  );

  return handleSchemaIdRemapping(response.result ?? {}, schemaId);
}

export async function updateSchemaName(schemaId: string, newName: string) {
  const erdStore = getErdStore();
  const { database, schema } = validateAndGetSchema(schemaId);

  await withOptimisticUpdate(
    () => {
      const oldName = schema.name;
      erdStore.changeSchemaName(schemaId, newName);
      return oldName;
    },
    () =>
      updateSchemaNameAPI(schemaId, {
        database,
        schemaId,
        newName,
      }),
    (oldName) => erdStore.changeSchemaName(schemaId, oldName),
  );
}

export async function deleteSchema(schemaId: string) {
  const erdStore = getErdStore();
  const { database, schema } = validateAndGetSchema(schemaId);

  if (database.schemas.length <= 1) {
    throw new Error('Cannot delete the last schema');
  }

  await withOptimisticUpdate(
    () => {
      const schemaSnapshot = structuredClone(schema);
      erdStore.deleteSchema(schemaId);
      return schemaSnapshot;
    },
    () =>
      deleteSchemaAPI(schemaId, {
        database,
        schemaId,
      }),
    (schemaSnapshot) => erdStore.createSchema(schemaSnapshot),
  );
}
