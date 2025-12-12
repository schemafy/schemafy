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
  const database = erdStore.database;

  if (!database) {
    throw new Error('Database not loaded');
  }

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

  const realId = response.result?.schemas[schemaId];
  if (realId && realId !== schemaId) {
    erdStore.replaceSchemaId(schemaId, realId);
    return realId;
  }

  return schemaId;
}

export async function updateSchemaName(schemaId: string, newName: string) {
  const erdStore = getErdStore();
  const database = erdStore.database;

  if (!database) {
    throw new Error('Database not loaded');
  }

  const schema = findSchema(schemaId);
  if (!schema) {
    throw new Error(`Schema ${schemaId} not found`);
  }

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
  const database = erdStore.database;

  if (!database) {
    throw new Error('Database not loaded');
  }

  if (database.schemas.length <= 1) {
    throw new Error('Cannot delete the last schema');
  }

  const schema = findSchema(schemaId);
  if (!schema) {
    throw new Error(`Schema ${schemaId} not found`);
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

function findSchema(schemaId: string) {
  const erdStore = getErdStore();
  const database = erdStore.database;
  if (!database) return undefined;
  return database.schemas.find((s) => s.id === schemaId);
}
