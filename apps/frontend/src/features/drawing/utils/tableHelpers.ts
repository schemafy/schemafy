import type { Node } from '@xyflow/react';
import type { Constraint } from '@/types';
import type { TableData, ColumnType, ConstraintKind, Point } from '../types';
import type { TableSnapshotResponse } from '../api';

type TableExtra = {
  position?: Point;
};

type ConstraintMap = {
  primaryKeys: Set<string>;
  notNulls: Set<string>;
  uniques: Set<string>;
};

const buildConstraintMap = (constraints: Constraint[]): ConstraintMap => {
  const map: ConstraintMap = {
    primaryKeys: new Set(),
    notNulls: new Set(),
    uniques: new Set(),
  };

  constraints.forEach((constraint) => {
    const columnIds = constraint.columns.map((c) => c.columnId);

    switch (constraint.kind) {
      case 'PRIMARY_KEY':
        columnIds.forEach((id) => map.primaryKeys.add(id));
        break;
      case 'NOT_NULL':
        if (columnIds.length === 1) {
          map.notNulls.add(columnIds[0]);
        }
        break;
      case 'UNIQUE':
        if (columnIds.length === 1) {
          map.uniques.add(columnIds[0]);
        }
        break;
    }
  });

  return map;
};

const transformColumnWithMap = (
  columnId: string,
  columnName: string,
  dataType: string,
  constraintMap: ConstraintMap,
  foreignKeyColumnIds: Set<string>,
): ColumnType => {
  const isPrimaryKey = constraintMap.primaryKeys.has(columnId);
  const isForeignKey = foreignKeyColumnIds.has(columnId);
  const isNotNull = constraintMap.notNulls.has(columnId);
  const isUnique = isPrimaryKey || constraintMap.uniques.has(columnId);

  return {
    id: columnId,
    name: columnName,
    type: dataType || 'VARCHAR',
    isPrimaryKey,
    isForeignKey,
    isNotNull,
    isUnique,
  };
};

export const transformSnapshotToNode = (
  snapshot: TableSnapshotResponse,
  schemaId: string,
): Node<TableData> => {
  let extra: TableExtra = {};
  if (snapshot.table.extra) {
    try {
      extra = JSON.parse(snapshot.table.extra) as TableExtra;
    } catch {
      extra = {};
    }
  }
  const position = extra.position || { x: 0, y: 0 };

  const constraints: Constraint[] = snapshot.constraints.map((c) => ({
    ...c.constraint,
    kind: c.constraint.kind as ConstraintKind,
    columns: c.columns.map((col) => ({ ...col, isAffected: false })),
    isAffected: false,
  }));

  const constraintMap = buildConstraintMap(constraints);

  const foreignKeyColumnIds = new Set<string>();
  snapshot.relationships.forEach((rel) => {
    if (rel.relationship.fkTableId === snapshot.table.id) {
      rel.columns.forEach((col) => foreignKeyColumnIds.add(col.fkColumnId));
    }
  });

  const columns = snapshot.columns.map((col) =>
    transformColumnWithMap(
      col.id,
      col.name,
      col.dataType,
      constraintMap,
      foreignKeyColumnIds,
    ),
  );

  return {
    id: snapshot.table.id,
    type: 'table',
    position,
    data: {
      tableName: snapshot.table.name,
      columns,
      indexes: [],
      constraints,
      schemaId,
    },
  };
};
