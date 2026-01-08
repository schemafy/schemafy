import type { ErdStore } from '@/store/erd.store';
import type { SyncContext, EntityType } from '../../types';
import type { AffectedMappingResponse } from '../../api/types/common';
import type {
  Column,
  Index,
  IndexColumn,
  Constraint,
  ConstraintColumn,
  Relationship,
  RelationshipColumn,
  Table,
  Schema,
} from '@schemafy/validator';
import { syncTempToRealIds } from './idSynchronizer';
import { syncPropagatedEntities } from './propagation/propagationManager';

export type IdMappingWithType = {
  realId: string;
  type: EntityType;
};

export function handleServerResponse(
  response: AffectedMappingResponse,
  context: SyncContext,
  erdStore: ErdStore,
) {
  const idMap = new Map<string, IdMappingWithType>();

  syncTempToRealIds(response, context, erdStore, idMap);

  if (response.propagated) {
    syncPropagatedEntities(response.propagated, context, erdStore, idMap);
  }

  return idMap;
}

export function matchLocalPropagatedEntities(
  propagated: AffectedMappingResponse['propagated'],
  context: SyncContext,
  localStore: ErdStore,
  idMap: Map<string, IdMappingWithType>,
) {
  if (!propagated) return;

  syncPropagatedEntities(propagated, context, localStore, idMap);
}

export function applyLocalIdMapping(
  idMap: Map<string, IdMappingWithType>,
  localStore: ErdStore,
) {
  if (localStore.erdState.state !== 'loaded' || idMap.size === 0) {
    return;
  }

  const db = localStore.database!;
  const updatedDb = {
    ...db,
    schemas: db.schemas.map((schema) => mapSchema(schema, idMap)),
  };

  localStore.load(updatedDb);
}

const replaceId = (
  currentId: string,
  idMap: Map<string, IdMappingWithType>,
  expectedType: EntityType,
) => {
  const mapping = idMap.get(currentId);
  return mapping?.type === expectedType ? mapping.realId : currentId;
};

const mapColumn = (col: Column, idMap: Map<string, IdMappingWithType>) => ({
  ...col,
  id: replaceId(col.id, idMap, 'column'),
});

const mapIndexColumn = (
  indexColumn: IndexColumn,
  newIndexId: string,
  idMap: Map<string, IdMappingWithType>,
) => ({
  ...indexColumn,
  id: replaceId(indexColumn.id, idMap, 'indexColumn'),
  indexId: newIndexId,
  columnId: replaceId(indexColumn.columnId, idMap, 'column'),
});

const mapIndex = (index: Index, idMap: Map<string, IdMappingWithType>) => {
  const newIndexId = replaceId(index.id, idMap, 'index');
  return {
    ...index,
    id: newIndexId,
    columns: index.columns.map((ic) => mapIndexColumn(ic, newIndexId, idMap)),
  };
};

const mapConstraintColumn = (
  constraintColumn: ConstraintColumn,
  newConstraintId: string,
  idMap: Map<string, IdMappingWithType>,
) => ({
  ...constraintColumn,
  id: replaceId(constraintColumn.id, idMap, 'constraintColumn'),
  constraintId: newConstraintId,
  columnId: replaceId(constraintColumn.columnId, idMap, 'column'),
});

const mapConstraint = (
  constraint: Constraint,
  idMap: Map<string, IdMappingWithType>,
) => {
  const newConstraintId = replaceId(constraint.id, idMap, 'constraint');
  return {
    ...constraint,
    id: newConstraintId,
    columns: constraint.columns.map((cc) =>
      mapConstraintColumn(cc, newConstraintId, idMap),
    ),
  };
};

const mapRelationshipColumn = (
  relationshipColumn: RelationshipColumn,
  newRelationshipId: string,
  idMap: Map<string, IdMappingWithType>,
) => ({
  ...relationshipColumn,
  id: replaceId(relationshipColumn.id, idMap, 'relationshipColumn'),
  relationshipId: newRelationshipId,
  fkColumnId: replaceId(relationshipColumn.fkColumnId, idMap, 'column'),
  pkColumnId: replaceId(relationshipColumn.pkColumnId, idMap, 'column'),
});

const mapRelationship = (
  relationship: Relationship,
  idMap: Map<string, IdMappingWithType>,
) => {
  const newRelationshipId = replaceId(relationship.id, idMap, 'relationship');
  return {
    ...relationship,
    id: newRelationshipId,
    fkTableId: replaceId(relationship.fkTableId, idMap, 'table'),
    pkTableId: replaceId(relationship.pkTableId, idMap, 'table'),
    columns: relationship.columns.map((rc) =>
      mapRelationshipColumn(rc, newRelationshipId, idMap),
    ),
  };
};

const mapTable = (table: Table, idMap: Map<string, IdMappingWithType>) => ({
  ...table,
  id: replaceId(table.id, idMap, 'table'),
  columns: table.columns.map((col) => mapColumn(col, idMap)),
  indexes: table.indexes.map((idx) => mapIndex(idx, idMap)),
  constraints: table.constraints.map((constraint) =>
    mapConstraint(constraint, idMap),
  ),
  relationships: table.relationships.map((rel) => mapRelationship(rel, idMap)),
});

const mapSchema = (schema: Schema, idMap: Map<string, IdMappingWithType>) => ({
  ...schema,
  id: replaceId(schema.id, idMap, 'schema'),
  tables: schema.tables.map((table) => mapTable(table, idMap)),
});
