import type { Schema } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';

export const ERROR_MESSAGES = {
  DATABASE_NOT_LOADED: 'Database not loaded',
  SCHEMA_NOT_FOUND: (id: string) => `Schema ${id} not found`,
  TABLE_NOT_FOUND: (id: string) => `Table ${id} not found`,
  COLUMN_NOT_FOUND: (id: string) => `Column ${id} not found`,
  CONSTRAINT_NOT_FOUND: (id: string) => `Constraint ${id} not found`,
  RELATIONSHIP_NOT_FOUND: (id: string) => `Relationship ${id} not found`,
  RELATIONSHIP_COLUMN_NOT_FOUND: (id: string) =>
    `Relationship column ${id} not found`,
  INDEX_NOT_FOUND: (id: string) => `Index ${id} not found`,
} as const;

const getErdStore = () => ErdStore.getInstance();

export function validateDatabase() {
  const erdStore = getErdStore();
  const database = erdStore.database;

  if (!database) {
    throw new Error(ERROR_MESSAGES.DATABASE_NOT_LOADED);
  }

  return database;
}

export function validateAndGetSchema(schemaId: string) {
  const database = validateDatabase();
  const schema = database.schemas.find((s) => s.id === schemaId);

  if (!schema) {
    throw new Error(ERROR_MESSAGES.SCHEMA_NOT_FOUND(schemaId));
  }

  return { database, schema };
}

export function validateAndGetTable(schemaId: string, tableId: string) {
  const { database, schema } = validateAndGetSchema(schemaId);
  const table = schema.tables.find((t) => t.id === tableId);

  if (!table) {
    throw new Error(ERROR_MESSAGES.TABLE_NOT_FOUND(tableId));
  }

  return { database, schema, table };
}

export function validateAndGetColumn(
  schemaId: string,
  tableId: string,
  columnId: string,
) {
  const { database, schema, table } = validateAndGetTable(schemaId, tableId);
  const column = table.columns.find((c) => c.id === columnId);

  if (!column) {
    throw new Error(ERROR_MESSAGES.COLUMN_NOT_FOUND(columnId));
  }

  return { database, schema, table, column };
}

export function validateAndGetConstraint(
  schemaId: string,
  tableId: string,
  constraintId: string,
) {
  const { database, schema, table } = validateAndGetTable(schemaId, tableId);
  const constraint = table.constraints.find((c) => c.id === constraintId);

  if (!constraint) {
    throw new Error(ERROR_MESSAGES.CONSTRAINT_NOT_FOUND(constraintId));
  }

  return { database, schema, table, constraint };
}

export function validateAndGetRelationship(
  schemaId: string,
  relationshipId: string,
) {
  const { database, schema } = validateAndGetSchema(schemaId);
  const relationship = schema.tables
    .flatMap((t) => t.relationships)
    .find((r) => r.id === relationshipId);

  if (!relationship) {
    throw new Error(ERROR_MESSAGES.RELATIONSHIP_NOT_FOUND(relationshipId));
  }

  return { database, schema, relationship };
}

export function findTableInSchema(schema: Schema, tableId: string) {
  return schema.tables.find((t) => t.id === tableId);
}

export function validateAndGetIndex(
  schemaId: string,
  tableId: string,
  indexId: string,
) {
  const { database, schema, table } = validateAndGetTable(schemaId, tableId);
  const index = table.indexes.find((i) => i.id === indexId);

  if (!index) {
    throw new Error(ERROR_MESSAGES.INDEX_NOT_FOUND(indexId));
  }

  return { database, schema, table, index };
}

export function findRelationshipInSchema(
  schema: Schema,
  relationshipId: string,
) {
  return schema.tables
    .flatMap((t) => t.relationships)
    .find((r) => r.id === relationshipId);
}

export function findTableInDatabase(schemaId: string, tableId: string) {
  const erdStore = getErdStore();
  return erdStore.database?.schemas
    .find((s) => s.id === schemaId)
    ?.tables.find((t) => t.id === tableId);
}
