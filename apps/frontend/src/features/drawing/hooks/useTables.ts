import { useState, useEffect, useMemo, useCallback } from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import { ErdStore } from '@/store';
import type { Schema, Table, Column, Constraint } from '@schemafy/validator';
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
      schemaId,
    },
  };
};

export const useTables = () => {
  const erdStore = ErdStore.getInstance();

  const tablesFromStore = useMemo<Node<TableData>[]>(() => {
    if (erdStore.erdState.state !== 'loaded') return [];

    const { database } = erdStore.erdState;
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = database.schemas.find((s: Schema) => s.id === selectedSchemaId);

    if (!selectedSchema) return [];

    return selectedSchema.tables.map((table) => transformTableToNode(table, selectedSchema.id));
  }, [erdStore.erdState, erdStore.selectedSchemaId]);

  const [tables, setTables] = useState<Node<TableData>[]>(tablesFromStore);

  useEffect(() => {
    setTables(tablesFromStore);
  }, [tablesFromStore]);

  const addTable = (position: { x: number; y: number }) => {
    if (erdStore.erdState.state !== 'loaded') {
      console.error('Database is not loaded');
      return;
    }

    const selectedSchemaId = erdStore.selectedSchemaId;
    if (!selectedSchemaId) {
      console.error('No schema selected');
      return;
    }

    const selectedSchema = erdStore.erdState.database.schemas.find((s: Schema) => s.id === selectedSchemaId);
    if (!selectedSchema) {
      console.error('Selected schema not found');
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

  const onTablesChange = useCallback(
    (changes: NodeChange[]) => {
      setTables((nds) => {
        const updatedTables = applyNodeChanges(changes, nds) as Node<TableData>[];

        if (erdStore.erdState.state !== 'loaded') return updatedTables;

        const { database } = erdStore.erdState;

        changes
          .filter((change) => change.type === 'position' && change.dragging === false && change.position)
          .forEach((change) => {
            if (change.type !== 'position' || !change.position) return;

            const table = nds.find((t) => t.id === change.id);
            if (!table) return;

            const schemaId = table.data.schemaId;
            const currentTable = database.schemas
              .find((s: Schema) => s.id === schemaId)
              ?.tables.find((t: Table) => t.id === change.id);

            if (currentTable) {
              const currentExtra = (currentTable.extra || {}) as TableExtra;
              erdStore.updateTableExtra(schemaId, change.id, {
                ...currentExtra,
                position: change.position,
              });
            }
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
    },
    [erdStore],
  );

  return {
    tables,
    addTable,
    onTablesChange,
  };
};
