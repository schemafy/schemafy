import { ErdStore } from '@/store/erd.store';
import type { ServerResponse, SyncContext } from '../../types';

export function syncTempToRealIds(
  result: NonNullable<ServerResponse['result']>,
  ctx: SyncContext,
) {
  const erdStore = ErdStore.getInstance();
  const { schemaId, tableId, relationshipId, constraintId, indexId } = ctx;

  replaceFlatIds(result.schemas, (t, r) => erdStore.replaceSchemaId(t, r));
  replaceFlatIds(result.tables, (t, r) =>
    erdStore.replaceTableId(schemaId, t, r),
  );

  if (tableId) {
    replaceNestedIds(result.columns, tableId, (t, r) =>
      erdStore.replaceColumnId(schemaId, tableId, t, r),
    );
    replaceNestedIds(result.constraints, tableId, (t, r) =>
      erdStore.replaceConstraintId(schemaId, tableId, t, r),
    );
    replaceNestedIds(result.indexes, tableId, (t, r) =>
      erdStore.replaceIndexId(schemaId, tableId, t, r),
    );
    replaceNestedIds(result.relationships, tableId, (t, r) =>
      erdStore.replaceRelationshipId(schemaId, t, r),
    );
  }

  if (relationshipId) {
    replaceNestedIds(result.relationshipColumns, relationshipId, (t, r) =>
      erdStore.replaceRelationshipColumnId(schemaId, relationshipId, t, r),
    );
  }

  if (constraintId && tableId) {
    replaceNestedIds(result.constraintColumns, constraintId, (t, r) =>
      erdStore.replaceConstraintColumnId(schemaId, tableId, constraintId, t, r),
    );
  }

  if (indexId && tableId) {
    replaceNestedIds(result.indexColumns, indexId, (t, r) =>
      erdStore.replaceIndexColumnId(schemaId, tableId, indexId, t, r),
    );
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
