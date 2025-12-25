import type { PropagatedEntities } from '../api/types/common';
import { ErdStore } from '@/store/erd.store';
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

  const propConstColsByConstraintId = new Map<
    string,
    (typeof propagated.constraintColumns)[0][]
  >();

  propagated.constraintColumns.forEach((propConstCol) => {
    if (propConstCol.sourceId === finalRelationshipId) {
      if (!propConstColsByConstraintId.has(propConstCol.constraintId)) {
        propConstColsByConstraintId.set(propConstCol.constraintId, []);
      }
      propConstColsByConstraintId
        .get(propConstCol.constraintId)!
        .push(propConstCol);
    }
  });

  propConstColsByConstraintId.forEach((propConstCols, realConstraintId) => {
    const propConstraint = propagated.constraints.find(
      (c) =>
        c.constraintId === realConstraintId &&
        c.sourceId === finalRelationshipId,
    );

    if (!propConstraint) {
      return;
    }

    const table = erdStore.database?.schemas
      .find((s) => s.id === schemaId)
      ?.tables.find((t) => t.id === propConstraint.tableId);

    if (!table) {
      return;
    }

    const tempConstraint = table.constraints.find(
      (c) => c.name === propConstraint.name && c.kind === propConstraint.kind,
    );

    if (!tempConstraint) {
      return;
    }

    const tempConstColsSnapshot = tempConstraint.columns.map((cc) => ({
      id: cc.id,
      columnId: cc.columnId,
      seqNo: cc.seqNo,
    }));

    const sortedTempConstCols = [...tempConstColsSnapshot].sort(
      (a, b) => a.seqNo - b.seqNo,
    );

    const sortedPropConstCols = [...propConstCols].sort((a, b) => {
      const aTempCol = tempConstColsSnapshot.find((tc) => {
        const realColumnId = tempToRealColumnIdMap.get(tc.columnId);
        return realColumnId === a.columnId;
      });
      const bTempCol = tempConstColsSnapshot.find((tc) => {
        const realColumnId = tempToRealColumnIdMap.get(tc.columnId);
        return realColumnId === b.columnId;
      });
      return (aTempCol?.seqNo ?? 0) - (bTempCol?.seqNo ?? 0);
    });

    sortedTempConstCols.forEach((tempConstCol, index) => {
      const propConstCol = sortedPropConstCols[index];

      if (!propConstCol) {
        return;
      }

      if (tempConstCol.id !== propConstCol.constraintColumnId) {
        erdStore.replaceConstraintColumnId(
          schemaId,
          table.id,
          tempConstraint.id,
          tempConstCol.id,
          propConstCol.constraintColumnId,
        );
      }

      if (tempConstCol.columnId !== propConstCol.columnId) {
        erdStore.replaceConstraintColumnColumnId(
          schemaId,
          table.id,
          tempConstraint.id,
          propConstCol.constraintColumnId,
          propConstCol.columnId,
        );
      }
    });
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
