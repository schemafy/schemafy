import type { Node } from '@xyflow/react';
import type { Constraint } from '@/types';
import type {
  TableData,
  ColumnType,
  ConstraintKind,
  Point,
  IndexDataType,
  IndexSortDir,
  IndexType,
} from '../types';
import type { TableSnapshotResponse } from '../api';

export type TableExtra = {
  position?: Point;
};

export const parseTableExtra = (extraString: string | null): TableExtra => {
  if (!extraString) return {};

  try {
    return JSON.parse(extraString) as TableExtra;
  } catch {
    return {};
  }
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
  const extra = parseTableExtra(snapshot.table.extra);
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

  const indexes: IndexDataType[] = snapshot.indexes.map((indexSnapshot) => ({
    id: indexSnapshot.index.id,
    tableId: indexSnapshot.index.tableId,
    name: indexSnapshot.index.name,
    type: indexSnapshot.index.type as IndexType,
    columns: indexSnapshot.columns.map((col) => ({
      id: col.id,
      indexId: col.indexId,
      columnId: col.columnId,
      seqNo: col.seqNo,
      sortDir: col.sortDirection as IndexSortDir,
      isAffected: false,
    })),
    isAffected: false,
  }));

  return {
    id: snapshot.table.id,
    type: 'table',
    position,
    data: {
      tableName: snapshot.table.name,
      columns,
      indexes,
      constraints,
      schemaId,
    },
  };
};
