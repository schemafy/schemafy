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

export function applyLocalIdMapping(
  idMap: Map<string, IdMappingWithType>,
  localStore: ErdStore,
  syncedStore?: ErdStore,
) {
  if (localStore.erdState.state !== 'loaded' || idMap.size === 0) {
    return;
  }

  if (syncedStore?.erdState.state === 'loaded') {
    reconcileMissingMappings(idMap, localStore, syncedStore);
  }

  const db = localStore.database!;
  const updatedDb = {
    ...db,
    schemas: db.schemas.map((schema) => mapSchema(schema, idMap)),
  };

  localStore.load(updatedDb);
}

function reconcileMissingMappings(
  idMap: Map<string, IdMappingWithType>,
  localStore: ErdStore,
  syncedStore: ErdStore,
) {
  const syncedDb = syncedStore.database!;
  const localDb = localStore.database!;

  const syncedMap = buildSyncedDbMap(syncedDb);

  const realIdToMappingMap = new Map<string, IdMappingWithType>();
  for (const mapping of idMap.values()) {
    realIdToMappingMap.set(mapping.realId, mapping);
  }

  for (const localSchema of localDb.schemas) {
    const syncedSchema = syncedMap.get(localSchema.id);
    if (!syncedSchema) continue;

    for (const localTable of localSchema.tables) {
      const syncedTable = syncedSchema.tables.get(localTable.id);
      if (!syncedTable) continue;

      reconcileColumns(localTable, syncedTable, idMap, realIdToMappingMap);
      reconcileRelationships(
        localTable,
        syncedTable,
        idMap,
        realIdToMappingMap,
      );
    }
  }
}

function buildSyncedDbMap(db: { schemas: Schema[] }) {
  return new Map(
    db.schemas.map((s) => [
      s.id,
      {
        raw: s,
        tables: new Map(s.tables.map((t) => [t.id, t])),
      },
    ]),
  );
}

function reconcileColumns(
  localTable: Table,
  syncedTable: Table,
  idMap: Map<string, IdMappingWithType>,
  realIdToMappingMap: Map<string, IdMappingWithType>,
) {
  localTable.columns.forEach((localCol, idx) => {
    if (idMap.has(localCol.id)) return;

    const syncedCol = syncedTable.columns[idx];

    if (
      syncedCol &&
      syncedCol.name === localCol.name &&
      syncedCol.id !== localCol.id
    ) {
      const existingMapping = realIdToMappingMap.get(syncedCol.id);

      const newMapping = {
        realId: existingMapping?.realId ?? syncedCol.id,
        type: 'column' as const,
      };

      idMap.set(localCol.id, newMapping);
      realIdToMappingMap.set(newMapping.realId, newMapping);
    }
  });
}

function reconcileRelationships(
  localTable: Table,
  syncedTable: Table,
  idMap: Map<string, IdMappingWithType>,
  realIdToMappingMap: Map<string, IdMappingWithType>,
) {
  const syncedRels = new Map(syncedTable.relationships.map((r) => [r.id, r]));

  localTable.relationships.forEach((localRel) => {
    const syncedRel = syncedRels.get(localRel.id);
    if (!syncedRel) return;

    const syncedRelColsBySeqNo = new Map(
      syncedRel.columns.map((rc) => [rc.seqNo, rc]),
    );

    localRel.columns.forEach((localRelCol) => {
      if (idMap.has(localRelCol.fkColumnId)) return;

      const syncedRelCol = syncedRelColsBySeqNo.get(localRelCol.seqNo);
      if (!syncedRelCol) return;

      const existingMapping = realIdToMappingMap.get(syncedRelCol.fkColumnId);

      const newMapping = {
        realId: existingMapping?.realId ?? syncedRelCol.fkColumnId,
        type: 'column' as const,
      };

      idMap.set(localRelCol.fkColumnId, newMapping);
      realIdToMappingMap.set(newMapping.realId, newMapping);
    });
  });
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
