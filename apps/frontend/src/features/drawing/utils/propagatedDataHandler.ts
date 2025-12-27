import type { PropagatedEntities } from '../api/types/common';
import type { Constraint, Schema, Table } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';

type PropagatedData = {
  columns: PropagatedEntities['columns'];
  constraints: PropagatedEntities['constraints'];
  constraintColumns: PropagatedEntities['constraintColumns'];
  relationshipColumns: PropagatedEntities['relationshipColumns'];
};

export function handlePropagatedData(
  propagated: PropagatedEntities | undefined,
  schemaId: string,
  srcTableId: string,
  sourceId: string,
) {
  if (!propagated) return;

  const erdStore = ErdStore.getInstance();
  const schema = findSchema(erdStore, schemaId);
  if (!schema) return;

  const table = schema.tables.find((t) => t.id === srcTableId);
  const filteredData = filterPropagatedDataBySource(propagated, sourceId);

  handleRelationshipColumns(erdStore, schemaId, table, filteredData);

  handleStandaloneColumns(erdStore, schemaId, table, sourceId, filteredData);

  handleConstraintsAndColumns(erdStore, schemaId, schema, filteredData);
}

function findSchema(erdStore: ErdStore, schemaId: string): Schema | undefined {
  const schema = erdStore.database?.schemas.find((s) => s.id === schemaId);
  if (!schema) {
    console.error(`Schema ${schemaId} not found in database`);
  }
  return schema;
}

function filterPropagatedDataBySource(
  propagated: PropagatedEntities,
  sourceId: string,
): PropagatedData {
  return {
    columns: propagated.columns.filter((c) => c.sourceId === sourceId),
    constraints: propagated.constraints.filter((c) => c.sourceId === sourceId),
    constraintColumns: propagated.constraintColumns.filter(
      (c) => c.sourceId === sourceId,
    ),
    relationshipColumns: propagated.relationshipColumns.filter(
      (c) => c.sourceId === sourceId,
    ),
  };
}

function handleRelationshipColumns(
  erdStore: ErdStore,
  schemaId: string,
  table: Table | undefined,
  data: PropagatedData,
) {
  const { relationshipColumns, columns } = data;
  if (relationshipColumns.length === 0) return;

  const relationshipId = relationshipColumns[0].relationshipId;
  const relationship = table?.relationships.find(
    (r) => r.id === relationshipId,
  );

  if (!relationship) return;

  relationshipColumns.forEach((propRelCol) => {
    syncRelationshipColumn(
      erdStore,
      schemaId,
      relationshipId,
      relationship,
      propRelCol,
      columns,
    );
  });
}

function syncRelationshipColumn(
  erdStore: ErdStore,
  schemaId: string,
  relationshipId: string,
  relationship: {
    columns: { id: string; refColumnId: string; fkColumnId: string }[];
  },
  propRelCol: PropagatedData['relationshipColumns'][number],
  propColumns: PropagatedData['columns'],
) {
  const existingRelCol = relationship.columns.find(
    (rc) => rc.refColumnId === propRelCol.refColumnId,
  );

  if (!existingRelCol) return;

  erdStore.replaceRelationshipColumnId(
    schemaId,
    relationshipId,
    existingRelCol.id,
    propRelCol.relationshipColumnId,
  );

  const propCol = propColumns.find(
    (pc) => pc.sourceColumnId === propRelCol.refColumnId,
  );

  if (propCol && existingRelCol.fkColumnId !== propRelCol.fkColumnId) {
    erdStore.replaceRelationshipColumnFkId(
      schemaId,
      relationshipId,
      propRelCol.relationshipColumnId,
      propRelCol.fkColumnId,
    );

    erdStore.replaceColumnId(
      schemaId,
      propCol.tableId,
      existingRelCol.fkColumnId,
      propRelCol.fkColumnId,
    );
  }
}

function handleStandaloneColumns(
  erdStore: ErdStore,
  schemaId: string,
  table: Table | undefined,
  sourceId: string,
  data: PropagatedData,
) {
  const { columns, relationshipColumns } = data;
  if (columns.length === 0 || relationshipColumns.length > 0) return;

  const relationship = table?.relationships.find((r) => r.id === sourceId);
  if (!relationship) return;

  columns.forEach((propCol) => {
    syncStandaloneColumn(erdStore, schemaId, sourceId, relationship, propCol);
  });
}

function syncStandaloneColumn(
  erdStore: ErdStore,
  schemaId: string,
  relationshipId: string,
  relationship: {
    columns: { id: string; refColumnId: string; fkColumnId: string }[];
  },
  propCol: PropagatedData['columns'][number],
) {
  const relCol = relationship.columns.find(
    (rc) => rc.refColumnId === propCol.sourceColumnId,
  );

  if (!relCol || relCol.fkColumnId === propCol.columnId) return;

  erdStore.replaceRelationshipColumnFkId(
    schemaId,
    relationshipId,
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

function handleConstraintsAndColumns(
  erdStore: ErdStore,
  schemaId: string,
  schema: Schema,
  data: PropagatedData,
) {
  const { constraintColumns, constraints, columns } = data;
  if (constraintColumns.length === 0) return;

  const targetTableId = findTargetTableId(columns, constraints);
  if (!targetTableId) return;

  const targetTable = schema.tables.find((t) => t.id === targetTableId);
  if (!targetTable) return;

  if (constraints.length > 0) {
    syncConstraints(erdStore, schemaId, targetTable, constraints);
  }

  const updatedSchema = erdStore.database?.schemas.find(
    (s) => s.id === schemaId,
  );
  const updatedTable = updatedSchema?.tables.find(
    (t) => t.id === targetTable.id,
  );

  if (updatedTable) {
    syncConstraintsColumns(erdStore, schemaId, updatedTable, constraintColumns);
  }
}

function findTargetTableId(
  columns: PropagatedData['columns'],
  constraints: PropagatedData['constraints'],
): string | undefined {
  if (columns.length > 0) {
    return columns[0].tableId;
  }
  if (constraints.length > 0) {
    return constraints[0].tableId;
  }
  return undefined;
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
