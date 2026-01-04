import type { ErdStore } from '@/store/erd.store';
import type { SyncContext, EntityType } from '../../types';
import type { AffectedMappingResponse } from '../../api/types/common';
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
): Map<string, IdMappingWithType> {
  const idMap = new Map<string, IdMappingWithType>();

  syncTempToRealIds(response, context, erdStore, idMap);

  if (response.propagated) {
    syncPropagatedEntities(response.propagated, context, erdStore, idMap);
  }

  return idMap;
}

const replaceId = (
  currentId: string,
  idMap: Map<string, IdMappingWithType>,
  expectedType: EntityType,
): string => {
  const mapping = idMap.get(currentId);
  return mapping?.type === expectedType ? mapping.realId : currentId;
};

export function applyLocalIdMapping(
  idMap: Map<string, IdMappingWithType>,
  erdStore: ErdStore,
) {
  if (erdStore.erdState.state !== 'loaded') {
    return;
  }

  const db = erdStore.database!;

  const updatedDb = {
    ...db,
    schemas: db.schemas.map((schema) => ({
      ...schema,
      id: replaceId(schema.id, idMap, 'schema'),
      tables: schema.tables.map((table) => {
        const newTableId = replaceId(table.id, idMap, 'table');
        return {
          ...table,
          id: newTableId,
          columns: table.columns.map((col) => ({
            ...col,
            id: replaceId(col.id, idMap, 'column'),
          })),
          indexes: table.indexes.map((idx) => {
            const newIdxId = replaceId(idx.id, idMap, 'index');
            return {
              ...idx,
              id: newIdxId,
              columns: idx.columns.map((ic) => ({
                ...ic,
                id: replaceId(ic.id, idMap, 'indexColumn'),
              })),
            };
          }),
          constraints: table.constraints.map((constraint) => {
            const newConstraintId = replaceId(
              constraint.id,
              idMap,
              'constraint',
            );
            return {
              ...constraint,
              id: newConstraintId,
              columns: constraint.columns.map((cc) => ({
                ...cc,
                id: replaceId(cc.id, idMap, 'constraintColumn'),
                constraintId: newConstraintId,
              })),
            };
          }),
          relationships: table.relationships.map((rel) => {
            const newRelId = replaceId(rel.id, idMap, 'relationship');
            return {
              ...rel,
              id: newRelId,
              columns: rel.columns.map((rc) => ({
                ...rc,
                id: replaceId(rc.id, idMap, 'relationshipColumn'),
                relationshipId: newRelId,
              })),
            };
          }),
        };
      }),
    })),
  };

  erdStore.load(updatedDb);
}
