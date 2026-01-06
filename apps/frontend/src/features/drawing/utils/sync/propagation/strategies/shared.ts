import type { ErdStore } from '@/store/erd.store';
import type {
  PropagatedEntitiesGroup,
  SyncContext,
} from '@/features/drawing/types';
import type {
  PropagatedColumn,
  PropagatedRelationshipColumn,
  PropagatedConstraintColumn,
} from '@/features/drawing/api/types/common';
import type { Schema } from '@schemafy/validator';
import type { IdMappingWithType } from '../../index';
import { findSchema } from '../../helpers';

export function replacePropagatedColumns(
  columns: PropagatedColumn[],
  schema: Schema,
  context: SyncContext,
  erdStore: ErdStore,
  idMap: Map<string, IdMappingWithType>,
) {
  const tableMap = new Map(schema.tables.map((t) => [t.id, t]));

  columns.forEach((propCol) => {
    const table = tableMap.get(propCol.tableId);
    if (!table) return;

    for (const rel of table.relationships) {
      const relCol = rel.columns.find(
        (rc) => rc.pkColumnId === propCol.sourceColumnId,
      );
      if (!relCol) continue;

      const existingCol = table.columns.find(
        (col) => col.id === relCol.fkColumnId,
      );
      if (!existingCol) continue;

      if (existingCol.id !== propCol.columnId) {
        erdStore.replaceColumnId(
          context.schemaId,
          propCol.tableId,
          existingCol.id,
          propCol.columnId,
        );
        idMap.set(existingCol.id, { realId: propCol.columnId, type: 'column' });

        erdStore.replaceRelationshipColumnFkId(
          context.schemaId,
          rel.id,
          relCol.id,
          propCol.columnId,
        );
      }

      break;
    }
  });
}

export function replacePropagatedRelationshipColumns(
  relationshipColumns: PropagatedRelationshipColumn[],
  schema: Schema,
  context: SyncContext,
  erdStore: ErdStore,
  idMap: Map<string, IdMappingWithType>,
) {
  const relationshipMap = new Map<
    string,
    {
      table: Schema['tables'][number];
      relationship: Schema['tables'][number]['relationships'][number];
    }
  >();

  schema.tables.forEach((table) => {
    table.relationships.forEach((rel) => {
      relationshipMap.set(rel.id, { table, relationship: rel });
    });
  });

  relationshipColumns.forEach((propRelCol) => {
    const found = relationshipMap.get(propRelCol.relationshipId);
    if (!found) return;

    const { relationship } = found;

    const existingRelCol = relationship.columns.find(
      (rc) => rc.seqNo === propRelCol.seqNo,
    );
    if (!existingRelCol) return;

    erdStore.replaceRelationshipColumnId(
      context.schemaId,
      propRelCol.relationshipId,
      existingRelCol.id,
      propRelCol.relationshipColumnId,
    );
    idMap.set(existingRelCol.id, {
      realId: propRelCol.relationshipColumnId,
      type: 'relationshipColumn',
    });

    if (existingRelCol.fkColumnId !== propRelCol.fkColumnId) {
      erdStore.replaceRelationshipColumnFkId(
        context.schemaId,
        propRelCol.relationshipId,
        propRelCol.relationshipColumnId,
        propRelCol.fkColumnId,
      );
    }
  });
}

export function replaceConstraintIds(
  entities: Pick<
    PropagatedEntitiesGroup,
    'columns' | 'constraints' | 'constraintColumns'
  >,
  context: SyncContext,
  erdStore: ErdStore,
  idMap: Map<string, IdMappingWithType>,
) {
  if (
    entities.constraints.length === 0 &&
    entities.constraintColumns.length === 0
  ) {
    return;
  }

  const targetTableId =
    entities.columns[0]?.tableId || entities.constraints[0]?.tableId;
  if (!targetTableId) return;

  const schema = findSchema(erdStore, context.schemaId);
  const targetTable = schema.tables.find((t) => t.id === targetTableId);
  if (!targetTable) return;

  entities.constraints.forEach((propConstraint) => {
    const existing = targetTable.constraints.find(
      (c) => c.name === propConstraint.name && c.kind === propConstraint.kind,
    );
    if (existing) {
      erdStore.replaceConstraintId(
        context.schemaId,
        targetTableId,
        existing.id,
        propConstraint.constraintId,
      );
      idMap.set(existing.id, {
        realId: propConstraint.constraintId,
        type: 'constraint',
      });
    }
  });

  if (entities.constraintColumns.length > 0) {
    const updatedSchema = findSchema(erdStore, context.schemaId);
    const updatedTable = updatedSchema.tables.find(
      (t) => t.id === targetTableId,
    );
    if (!updatedTable) return;

    replaceConstraintColumnIds(
      updatedTable,
      entities.constraintColumns,
      context,
      targetTableId,
      erdStore,
      idMap,
    );
  }
}

function replaceConstraintColumnIds(
  table: {
    constraints: Array<{
      id: string;
      columns: Array<{ id: string; columnId: string }>;
    }>;
  },
  constraintColumns: PropagatedConstraintColumn[],
  context: SyncContext,
  tableId: string,
  erdStore: ErdStore,
  idMap: Map<string, IdMappingWithType>,
) {
  const grouped = constraintColumns.reduce(
    (acc, cc) => {
      if (!acc[cc.constraintId]) acc[cc.constraintId] = [];
      acc[cc.constraintId].push(cc);
      return acc;
    },
    {} as Record<string, PropagatedConstraintColumn[]>,
  );

  Object.entries(grouped).forEach(([constraintId, propCols]) => {
    const constraint = table.constraints.find((c) => c.id === constraintId);
    if (!constraint) return;

    const tempCols = constraint.columns.slice(
      constraint.columns.length - propCols.length,
    );

    tempCols.forEach((tempCol, index) => {
      const propCol = propCols[index];
      if (!propCol) return;

      if (tempCol.id !== propCol.constraintColumnId) {
        erdStore.replaceConstraintColumnId(
          context.schemaId,
          tableId,
          constraintId,
          tempCol.id,
          propCol.constraintColumnId,
        );
        idMap.set(tempCol.id, {
          realId: propCol.constraintColumnId,
          type: 'constraintColumn',
        });
      }

      if (tempCol.columnId !== propCol.columnId) {
        erdStore.replaceConstraintColumnColumnId(
          context.schemaId,
          tableId,
          constraintId,
          propCol.constraintColumnId,
          propCol.columnId,
        );
      }
    });
  });
}
