import { ErdStore } from '@/store/erd.store';

type IdMappingResult = Record<string, string>;

const getErdStore = () => ErdStore.getInstance();

function applyIdRemapping(
  mapping: IdMappingResult | undefined,
  tempId: string,
  applyFn: (tempId: string, realId: string) => void,
) {
  const realId = mapping?.[tempId];

  if (realId && realId !== tempId) {
    applyFn(tempId, realId);
    return realId;
  }

  return tempId;
}

export function handleSchemaIdRemapping(
  result: { schemas?: IdMappingResult },
  tempId: string,
) {
  const erdStore = getErdStore();
  return applyIdRemapping(result.schemas, tempId, (temp, real) => {
    erdStore.replaceSchemaId(temp, real);
  });
}

export function handleTableIdRemapping(
  result: { tables?: IdMappingResult },
  schemaId: string,
  tempId: string,
) {
  const erdStore = getErdStore();
  return applyIdRemapping(result.tables, tempId, (temp, real) => {
    erdStore.replaceTableId(schemaId, temp, real);
  });
}

export function handleColumnIdRemapping(
  result: { columns?: Record<string, IdMappingResult> },
  schemaId: string,
  tableId: string,
  tempId: string,
) {
  const erdStore = getErdStore();
  return applyIdRemapping(result.columns?.[tableId], tempId, (temp, real) => {
    erdStore.replaceColumnId(schemaId, tableId, temp, real);
  });
}

export function handleConstraintIdRemapping(
  result: { constraints?: Record<string, IdMappingResult> },
  schemaId: string,
  tableId: string,
  tempId: string,
) {
  const erdStore = getErdStore();
  return applyIdRemapping(
    result.constraints?.[tableId],
    tempId,
    (temp, real) => {
      erdStore.replaceConstraintId(schemaId, tableId, temp, real);
    },
  );
}

export function handleConstraintColumnIdRemapping(
  result: { constraintColumns?: Record<string, IdMappingResult> },
  schemaId: string,
  tableId: string,
  constraintId: string,
  tempId: string,
) {
  const erdStore = getErdStore();
  return applyIdRemapping(
    result.constraintColumns?.[constraintId],
    tempId,
    (temp, real) => {
      erdStore.replaceConstraintColumnId(
        schemaId,
        tableId,
        constraintId,
        temp,
        real,
      );
    },
  );
}

export function handleRelationshipIdRemapping(
  result: { relationships?: Record<string, IdMappingResult> },
  schemaId: string,
  srcTableId: string,
  tempId: string,
) {
  const erdStore = getErdStore();
  return applyIdRemapping(
    result.relationships?.[srcTableId],
    tempId,
    (temp, real) => {
      erdStore.replaceRelationshipId(schemaId, temp, real);
    },
  );
}

export function handleRelationshipColumnIdRemapping(
  result: { relationshipColumns?: Record<string, IdMappingResult> },
  schemaId: string,
  relationshipId: string,
  tempId: string,
) {
  const erdStore = getErdStore();
  return applyIdRemapping(
    result.relationshipColumns?.[relationshipId],
    tempId,
    (temp, real) => {
      erdStore.replaceRelationshipColumnId(
        schemaId,
        relationshipId,
        temp,
        real,
      );
    },
  );
}

export function handleBatchRelationshipColumnRemapping(
  result: { relationshipColumns?: Record<string, IdMappingResult> },
  schemaId: string,
  relationshipId: string,
) {
  const erdStore = getErdStore();
  const mapping = result.relationshipColumns?.[relationshipId];

  if (!mapping) return;

  Object.entries(mapping).forEach(([tempId, realId]) => {
    if (tempId !== realId) {
      erdStore.replaceRelationshipColumnId(
        schemaId,
        relationshipId,
        tempId,
        realId,
      );
    }
  });
}

export function handleIndexIdRemapping(
  result: { indexes?: Record<string, IdMappingResult> },
  schemaId: string,
  tableId: string,
  tempId: string,
) {
  const erdStore = getErdStore();
  return applyIdRemapping(result.indexes?.[tableId], tempId, (temp, real) => {
    erdStore.replaceIndexId(schemaId, tableId, temp, real);
  });
}

export function handleIndexColumnIdRemapping(
  result: { indexColumns?: Record<string, IdMappingResult> },
  schemaId: string,
  tableId: string,
  indexId: string,
  tempId: string,
) {
  const erdStore = getErdStore();
  return applyIdRemapping(
    result.indexColumns?.[indexId],
    tempId,
    (temp, real) => {
      erdStore.replaceIndexColumnId(schemaId, tableId, indexId, temp, real);
    },
  );
}

export function handleBatchChildIdRemapping(
  result: Record<string, IdMappingResult> | undefined,
  parentId: string,
  replaceIdFn: (tempId: string, realId: string) => void,
) {
  const mapping = result?.[parentId];
  if (!mapping) return;

  Object.entries(mapping).forEach(([tempId, realId]) => {
    if (tempId !== realId) {
      replaceIdFn(tempId, realId);
    }
  });
}

export function createReverseMapping(mapping: IdMappingResult) {
  const reverseMap = new Map<string, string>();
  Object.entries(mapping).forEach(([tempId, realId]) => {
    reverseMap.set(realId, tempId);
  });
  return reverseMap;
}

export function findTempIdByRealId(mapping: IdMappingResult, realId: string) {
  for (const [tempId, mappedRealId] of Object.entries(mapping)) {
    if (mappedRealId === realId) {
      return tempId;
    }
  }
  return undefined;
}
