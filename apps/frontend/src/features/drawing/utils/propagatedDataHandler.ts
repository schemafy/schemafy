import type { PropagatedEntities } from '../api/types/common';
import type { Constraint, Relationship } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';

const getConstraintKey = (name: string, kind: string) => `${name}:${kind}`;

export function handlePropagatedData(
  propagated: PropagatedEntities | undefined,
  schemaId: string,
  srcTableId: string,
  finalRelationshipId: string,
) {
  if (!propagated) return;

  const erdStore = ErdStore.getInstance();

  const schema = erdStore.database?.schemas.find((s) => s.id === schemaId);
  if (!schema) {
    console.error(`Schema ${schemaId} not found in database`);
    return;
  }

  const table = schema.tables.find((t) => t.id === srcTableId);
  const relationship = table?.relationships.find(
    (r) => r.id === finalRelationshipId,
  );

  const propColumns = propagated.columns.filter(
    (c) => c.sourceId === finalRelationshipId,
  );
  const propConstraints = propagated.constraints.filter(
    (c) => c.sourceId === finalRelationshipId,
  );
  const propConstraintCols = propagated.constraintColumns.filter(
    (c) => c.sourceId === finalRelationshipId,
  );

  if (relationship && propColumns.length > 0) {
    syncRelationshipColumns(
      erdStore,
      schemaId,
      finalRelationshipId,
      relationship,
      propColumns,
    );
  }

  if (propConstraints.length > 0) {
    const targetTableId = propConstraints[0].tableId;
    const targetTable = schema.tables.find((t) => t.id === targetTableId);

    if (targetTable) {
      syncConstraints(
        erdStore,
        schemaId,
        targetTable,
        propConstraints,
        propConstraintCols,
      );
    }
  }
}

function syncRelationshipColumns(
  store: ErdStore,
  schemaId: string,
  finalRelationshipId: string,
  relationshipInDb: Relationship,
  propColumns: PropagatedEntities['columns'],
) {
  const relColMap = new Map(
    relationshipInDb.columns.map((rc) => [rc.refColumnId, rc]),
  );

  propColumns.forEach((propCol) => {
    const relCol = relColMap.get(propCol.sourceColumnId);

    if (relCol && relCol.fkColumnId !== propCol.columnId) {
      store.replaceRelationshipColumnFkId(
        schemaId,
        finalRelationshipId,
        relCol.id,
        propCol.columnId,
      );

      store.replaceColumnId(
        schemaId,
        propCol.tableId,
        relCol.fkColumnId,
        propCol.columnId,
      );
    }
  });
}

function syncConstraints(
  store: ErdStore,
  schemaId: string,
  targetTable: { id: string; constraints: Constraint[] },
  propConstraints: PropagatedEntities['constraints'],
  allPropConstraintCols: PropagatedEntities['constraintColumns'],
) {
  const existingConstraintMap = new Map(
    targetTable.constraints.map((c) => [getConstraintKey(c.name, c.kind), c]),
  );

  const propColsByConstraintId = new Map<
    string,
    PropagatedEntities['constraintColumns']
  >();
  allPropConstraintCols.forEach((pc) => {
    const cols = propColsByConstraintId.get(pc.constraintId);
    if (cols) {
      cols.push(pc);
    } else {
      propColsByConstraintId.set(pc.constraintId, [pc]);
    }
  });

  propConstraints.forEach((propConstraint) => {
    const key = getConstraintKey(propConstraint.name, propConstraint.kind);
    const existingConstraint = existingConstraintMap.get(key);

    if (!existingConstraint) return;

    if (existingConstraint.id !== propConstraint.constraintId) {
      store.replaceConstraintId(
        schemaId,
        propConstraint.tableId,
        existingConstraint.id,
        propConstraint.constraintId,
      );
    }

    const propConstCols = propColsByConstraintId.get(
      propConstraint.constraintId,
    );

    if (!propConstCols || propConstCols.length === 0) return;

    const sortedTempCols = [...existingConstraint.columns].sort(
      (a, b) => a.seqNo - b.seqNo,
    );

    sortedTempCols.forEach((tempCol, index) => {
      const propCol = propConstCols[index];
      if (!propCol) return;

      if (tempCol.id !== propCol.constraintColumnId) {
        store.replaceConstraintColumnId(
          schemaId,
          propConstraint.tableId,
          propConstraint.constraintId,
          tempCol.id,
          propCol.constraintColumnId,
        );
      }

      if (tempCol.columnId !== propCol.columnId) {
        store.replaceConstraintColumnColumnId(
          schemaId,
          propConstraint.tableId,
          propConstraint.constraintId,
          propCol.constraintColumnId,
          propCol.columnId,
        );
      }
    });
  });
}
