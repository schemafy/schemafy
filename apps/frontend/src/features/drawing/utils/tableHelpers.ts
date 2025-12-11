import type { Node } from '@xyflow/react';
import type { Table, Column, Constraint } from '@schemafy/validator';
import type { TableData, ColumnType, ConstraintKind, Point } from '../types';

type TableExtra = {
  position?: Point;
};

export const hasConstraint = (
  constraints: Constraint[],
  columnId: string,
  kind: ConstraintKind,
): boolean => {
  const checkSingleColumn = kind !== 'PRIMARY_KEY';

  return constraints.some((c) => {
    if (c.kind !== kind) return false;
    if (checkSingleColumn && c.columns.length !== 1) return false;
    return c.columns.some((cc) => cc.columnId === columnId);
  });
};

export const transformColumn = (
  col: Column,
  constraints: Constraint[],
): ColumnType => {
  const isPrimaryKey = hasConstraint(constraints, col.id, 'PRIMARY_KEY');
  const isNotNull = hasConstraint(constraints, col.id, 'NOT_NULL');
  const isUnique = isPrimaryKey || hasConstraint(constraints, col.id, 'UNIQUE');

  return {
    id: col.id,
    name: col.name,
    type: col.dataType || 'VARCHAR',
    isPrimaryKey,
    isNotNull,
    isUnique,
  };
};

export const transformTableToNode = (
  table: Table,
  schemaId: string,
): Node<TableData> => {
  const extra = (table.extra || {}) as TableExtra;
  const position = extra.position || { x: 0, y: 0 };
  const columns = table.columns.map((col) =>
    transformColumn(col, table.constraints),
  );

  return {
    id: table.id,
    type: 'table',
    position,
    data: {
      tableName: table.name,
      columns,
      indexes: table.indexes || [],
      constraints: table.constraints || [],
      schemaId,
    },
  };
};
