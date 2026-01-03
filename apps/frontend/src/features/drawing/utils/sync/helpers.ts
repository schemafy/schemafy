import type { ErdStore } from '@/store/erd.store';
import type { Schema, Table } from '@schemafy/validator';

export function findSchema(erdStore: ErdStore, schemaId: string) {
  const schema = erdStore.database?.schemas.find((s) => s.id === schemaId);
  if (!schema) {
    throw new Error(`Schema not found: ${schemaId}`);
  }
  return schema;
}

export function findTable(schema: Schema, tableId: string) {
  const table = schema.tables.find((t) => t.id === tableId);
  if (!table) {
    throw new Error(`Table not found: ${tableId}`);
  }
  return table;
}

export function findRelationship(table: Table, relationshipId: string) {
  const relationship = table.relationships.find((r) => r.id === relationshipId);
  if (!relationship) {
    throw new Error(`Relationship not found: ${relationshipId}`);
  }
  return relationship;
}

export function findTableByRelationship(
  schema: Schema,
  relationshipId: string,
) {
  return schema.tables.find((t) =>
    t.relationships.some((r) => r.id === relationshipId),
  );
}
