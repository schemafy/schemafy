import type { ErdStore } from '@/store/erd.store';
import type { SyncContext } from '../../types';
import type { AffectedMappingResponse } from '../../api/types/common';
import type { IdMappingWithType } from './index';

export function syncTempToRealIds(
  result: AffectedMappingResponse,
  ctx: SyncContext,
  erdStore: ErdStore,
  idMap: Map<string, IdMappingWithType>,
) {
  const { schemaId, tableId, relationshipId, constraintId, indexId } = ctx;

  replaceFlatIds(result.schemas, (t, r) => {
    erdStore.replaceSchemaId(t, r);
    idMap.set(t, { realId: r, type: 'schema' });
  });
  replaceFlatIds(result.tables, (t, r) => {
    erdStore.replaceTableId(schemaId, t, r);
    idMap.set(t, { realId: r, type: 'table' });
  });

  if (tableId) {
    replaceNestedIds(result.columns, tableId, (t, r) => {
      erdStore.replaceColumnId(schemaId, tableId, t, r);
      idMap.set(t, { realId: r, type: 'column' });
    });
    replaceNestedIds(result.constraints, tableId, (t, r) => {
      erdStore.replaceConstraintId(schemaId, tableId, t, r);
      idMap.set(t, { realId: r, type: 'constraint' });
    });
    replaceNestedIds(result.indexes, tableId, (t, r) => {
      erdStore.replaceIndexId(schemaId, tableId, t, r);
      idMap.set(t, { realId: r, type: 'index' });
    });
    replaceNestedIds(result.relationships, tableId, (t, r) => {
      erdStore.replaceRelationshipId(schemaId, t, r);
      idMap.set(t, { realId: r, type: 'relationship' });
    });
  }

  if (relationshipId) {
    replaceNestedIds(result.relationshipColumns, relationshipId, (t, r) => {
      erdStore.replaceRelationshipColumnId(schemaId, relationshipId, t, r);
      idMap.set(t, { realId: r, type: 'relationshipColumn' });
    });
  }

  if (constraintId && tableId) {
    const realConstraintId =
      result.constraints?.[tableId]?.[constraintId] || constraintId;

    replaceNestedIds(result.constraintColumns, realConstraintId, (t, r) => {
      erdStore.replaceConstraintColumnId(
        schemaId,
        tableId,
        realConstraintId,
        t,
        r,
      );
      idMap.set(t, { realId: r, type: 'constraintColumn' });
    });
  }

  if (indexId && tableId) {
    const realIndexId = result.indexes?.[tableId]?.[indexId] || indexId;

    replaceNestedIds(result.indexColumns, realIndexId, (t, r) => {
      erdStore.replaceIndexColumnId(schemaId, tableId, realIndexId, t, r);
      idMap.set(t, { realId: r, type: 'indexColumn' });
    });
  }
}

function replaceFlatIds(
  mapping: Record<string, string> | undefined,
  replaceFn: (tempId: string, realId: string) => void,
) {
  if (!mapping) return;
  Object.entries(mapping).forEach(([tempId, realId]) => {
    if (tempId !== realId) replaceFn(tempId, realId);
  });
}

function replaceNestedIds(
  mappings: Record<string, Record<string, string>> | undefined,
  parentId: string,
  replaceFn: (tempId: string, realId: string) => void,
) {
  if (!mappings?.[parentId]) return;
  Object.entries(mappings[parentId]).forEach(([tempId, realId]) => {
    if (tempId !== realId) replaceFn(tempId, realId);
  });
}
