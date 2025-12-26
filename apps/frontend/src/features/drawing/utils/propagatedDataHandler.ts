import type { PropagatedEntities } from '../api/types/common';
import type { Constraint, Relationship } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';

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

  if (propConstraintCols.length > 0) {
    const targetTable = schema.tables.find(
      (t) => t.id === propColumns[0].tableId,
    );

    if (targetTable) {
      if (propConstraints.length > 0) {
        syncConstraints(erdStore, schemaId, targetTable, propConstraints);
      }

      const updatedSchema = erdStore.database?.schemas.find(
        (s) => s.id === schemaId,
      );
      const updatedTable = updatedSchema?.tables.find(
        (t) => t.id === targetTable.id,
      );

      if (updatedTable) {
        syncConstraintsColumns(
          erdStore,
          schemaId,
          updatedTable,
          propConstraintCols,
        );
      }
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
) {
  propConstraints.forEach((propConstraint) => {
    const existingConstraint = targetTable.constraints.find(
      (c) => c.name === propConstraint.name && c.kind === propConstraint.kind,
    );

    if (existingConstraint) {
      store.replaceConstraintId(
        schemaId,
        targetTable.id,
        existingConstraint.id,
        propConstraint.constraintId,
      );
    }
  });
}

function syncConstraintsColumns(
  store: ErdStore,
  schemaId: string,
  targetTable: { id: string; constraints: Constraint[] },
  propConstraintCols: PropagatedEntities['constraintColumns'],
) {
  const groupedByConstraint = propConstraintCols.reduce(
    (acc, propCol) => {
      if (!acc[propCol.constraintId]) {
        acc[propCol.constraintId] = [];
      }
      acc[propCol.constraintId].push(propCol);
      return acc;
    },
    {} as Record<string, typeof propConstraintCols>,
  );

  Object.entries(groupedByConstraint).forEach(([constraintId, propCols]) => {
    const existingConstraint = targetTable.constraints.find(
      (c) => c.id === constraintId,
    );

    if (!existingConstraint) return;

    const tempCols = existingConstraint.columns.slice(
      existingConstraint.columns.length - propCols.length,
    );

    tempCols.forEach((tempCol, index) => {
      const propCol = propCols[index];
      if (!propCol) return;

      if (tempCol.id !== propCol.constraintColumnId) {
        store.replaceConstraintColumnId(
          schemaId,
          targetTable.id,
          constraintId,
          tempCol.id,
          propCol.constraintColumnId,
        );
      }

      if (tempCol.columnId !== propCol.columnId) {
        store.replaceConstraintColumnColumnId(
          schemaId,
          targetTable.id,
          constraintId,
          propCol.constraintColumnId,
          propCol.columnId,
        );
      }
    });
  });
}
