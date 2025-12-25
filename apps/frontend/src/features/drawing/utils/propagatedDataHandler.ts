import type { PropagatedEntities } from '../api/types/common';
import { ErdStore } from '@/store/erd.store';
import { findTempIdByRealId } from './idRemapping';
import { findTableInDatabase } from './entityValidators';

const getErdStore = () => ErdStore.getInstance();

export function handlePropagatedColumns(
  propagated: PropagatedEntities,
  schemaId: string,
  srcTableId: string,
  finalRelationshipId: string,
) {
  const erdStore = getErdStore();

  propagated.columns.forEach((propCol) => {
    if (propCol.sourceId === finalRelationshipId) {
      const relationshipInDb = erdStore.database?.schemas
        .find((s) => s.id === schemaId)
        ?.tables.find((t) => t.id === srcTableId)
        ?.relationships.find((r) => r.id === finalRelationshipId);

      if (relationshipInDb) {
        relationshipInDb.columns.forEach((relCol) => {
          if (relCol.refColumnId === propCol.sourceColumnId) {
            if (relCol.fkColumnId !== propCol.columnId) {
              erdStore.replaceRelationshipColumnFkId(
                schemaId,
                finalRelationshipId,
                relCol.id,
                propCol.columnId,
              );

              erdStore.replaceColumnId(
                schemaId,
                propCol.tableId,
                relCol.fkColumnId,
                propCol.columnId,
              );
            }
          }
        });
      }
    }
  });
}

export function handlePropagatedConstraints(
  propagated: PropagatedEntities,
  schemaId: string,
  finalRelationshipId: string,
) {
  const erdStore = getErdStore();
  const tempToRealColumnIdMap = new Map<string, string>();

  propagated.constraints.forEach((propConstraint) => {
    if (propConstraint.sourceId === finalRelationshipId) {
      const table = findTableInDatabase(schemaId, propConstraint.tableId);

      if (table) {
        const constraint = table.constraints.find(
          (c) =>
            c.name === propConstraint.name && c.kind === propConstraint.kind,
        );

        if (constraint) {
          const propConstCols = propagated.constraintColumns.filter(
            (pc) =>
              pc.sourceId === finalRelationshipId &&
              pc.constraintId === propConstraint.constraintId,
          );

          constraint.columns.forEach((constCol, index) => {
            const matchingPropConstCol = propConstCols[index];

            if (matchingPropConstCol) {
              tempToRealColumnIdMap.set(
                constCol.columnId,
                matchingPropConstCol.columnId,
              );
            }
          });
        }
      }
    }
  });

  propagated.constraints.forEach((propConstraint) => {
    if (propConstraint.sourceId === finalRelationshipId) {
      const table = findTableInDatabase(schemaId, propConstraint.tableId);

      if (table) {
        const tempConstraint = table.constraints.find(
          (c) =>
            c.name === propConstraint.name && c.kind === propConstraint.kind,
        );

        if (
          tempConstraint &&
          tempConstraint.id !== propConstraint.constraintId
        ) {
          erdStore.replaceConstraintId(
            schemaId,
            propConstraint.tableId,
            tempConstraint.id,
            propConstraint.constraintId,
          );
        }
      }
    }
  });

  return tempToRealColumnIdMap;
}

export function handlePropagatedConstraintColumns(
  propagated: PropagatedEntities,
  schemaId: string,
  finalRelationshipId: string,
  tempToRealColumnIdMap: Map<string, string>,
) {
  const erdStore = getErdStore();

  propagated.constraintColumns.forEach((propConstCol) => {
    if (propConstCol.sourceId === finalRelationshipId) {
      const table = erdStore.database?.schemas
        .find((s) => s.id === schemaId)
        ?.tables.find((t) =>
          t.constraints.some((c) => c.id === propConstCol.constraintId),
        );

      if (table) {
        const constraint = table.constraints.find(
          (c) => c.id === propConstCol.constraintId,
        );

        if (constraint) {
          const tempColumnId = findTempIdByRealId(
            Object.fromEntries(tempToRealColumnIdMap),
            propConstCol.columnId,
          );

          const tempConstCol = constraint.columns.find(
            (cc) => cc.columnId === tempColumnId,
          );

          if (tempConstCol) {
            if (tempConstCol.id !== propConstCol.constraintColumnId) {
              erdStore.replaceConstraintColumnId(
                schemaId,
                table.id,
                propConstCol.constraintId,
                tempConstCol.id,
                propConstCol.constraintColumnId,
              );
            }

            if (tempColumnId && tempColumnId !== propConstCol.columnId) {
              erdStore.replaceConstraintColumnColumnId(
                schemaId,
                table.id,
                propConstCol.constraintId,
                propConstCol.constraintColumnId,
                propConstCol.columnId,
              );
            }
          }
        }
      }
    }
  });
}

export function handlePropagatedData(
  propagated: PropagatedEntities | undefined,
  schemaId: string,
  srcTableId: string,
  finalRelationshipId: string,
) {
  if (!propagated) return;

  const tempToRealColumnIdMap = handlePropagatedConstraints(
    propagated,
    schemaId,
    finalRelationshipId,
  );

  handlePropagatedColumns(
    propagated,
    schemaId,
    srcTableId,
    finalRelationshipId,
  );

  handlePropagatedConstraintColumns(
    propagated,
    schemaId,
    finalRelationshipId,
    tempToRealColumnIdMap,
  );
}
