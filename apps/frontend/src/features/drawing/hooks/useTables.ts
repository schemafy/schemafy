import { useState, useEffect } from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import { ErdStore } from '@/store';
import type { Table, Column, Constraint } from '@schemafy/validator';
import type { TableData, ColumnType, ConstraintKind } from '../types';
import { ulid } from 'ulid';

type TableExtra = {
  position?: { x: number; y: number };
};

const hasConstraint = (constraints: Constraint[], columnId: string, kind: ConstraintKind): boolean => {
  return constraints.some((c) => c.kind === kind && c.columns.some((cc) => cc.columnId === columnId));
};

const transformColumn = (col: Column, constraints: Constraint[]): ColumnType => {
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

const transformTableToNode = (table: Table, schemaId: string): Node<TableData> => {
  const extra = (table.extra || {}) as TableExtra;
  const position = extra.position || { x: 0, y: 0 };
  const columns = table.columns.map((col) => transformColumn(col, table.constraints));

  return {
    id: table.id,
    type: 'table',
    position,
    data: {
      tableName: table.name,
      columns,
      indexes: table.indexes || [],
      schemaId,
    },
  };
};

export const useTables = () => {
  const erdStore = ErdStore.getInstance();

  const getTablesFromStore = (): Node<TableData>[] => {
    const selectedSchema = erdStore.selectedSchema;

    if (!selectedSchema) return [];

    return selectedSchema.tables.map((table) => transformTableToNode(table, selectedSchema.id));
  };

  const [tables, setTables] = useState<Node<TableData>[]>(getTablesFromStore());

  useEffect(() => {
    setTables(getTablesFromStore());
  }, [erdStore.erdState, erdStore.selectedSchemaId]);

  const addTable = (position: { x: number; y: number }) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = erdStore.selectedSchema;

    if (!selectedSchemaId || !selectedSchema) {
      console.error('No schema selected');
      return;
    }

    const tableCount = selectedSchema.tables.length;
    erdStore.createTable(selectedSchemaId, {
      id: ulid(),
      name: `Table_${tableCount + 1}`,
      columns: [],
      indexes: [],
      constraints: [],
      relationships: [],
      tableOptions: '',
      extra: { position },
    });
  };

  const onTablesChange = (changes: NodeChange[]) => {
    setTables((nds) => {
      const updatedTables = applyNodeChanges(changes, nds) as Node<TableData>[];

      changes
        .filter((change) => change.type === 'position' && change.dragging === false && change.position)
        .forEach((change) => {
          if (change.type !== 'position' || !change.position) return;

          const table = nds.find((t) => t.id === change.id);
          if (!table) return;

          const schemaId = table.data.schemaId;

          erdStore.updateTableExtra(schemaId, change.id, {
            position: change.position,
          });
        });

      changes
        .filter((change) => change.type === 'remove')
        .forEach((change) => {
          const table = nds.find((t) => t.id === change.id);
          if (table?.data.schemaId) {
            erdStore.deleteTable(table.data.schemaId, change.id);
          }
        });

      return updatedTables;
    });
  };

  return {
    tables,
    addTable,
    onTablesChange,
  };
};
