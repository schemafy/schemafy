import { useState, useCallback, useEffect } from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import { ErdStore } from '@/store';
import type { Schema, Table } from '@schemafy/validator';
import type { TableData } from '../types';

type TableExtra = {
  position?: { x: number; y: number };
};

export const useTables = () => {
  const erdStore = ErdStore.getInstance();

  const tablesFromStore: Node<TableData>[] =
    erdStore.erdState.state === 'loaded'
      ? erdStore.erdState.database.schemas.flatMap((schema: Schema) =>
          schema.tables.map((table: Table): Node<TableData> => {
            const extra = (table.extra || {}) as TableExtra;
            const position = extra.position || { x: 0, y: 0 };

            return {
              id: table.id,
              type: 'table',
              position,
              data: {
                tableName: table.name,
                columns: [], // TODO: column 데이터 매핑
                schemaId: schema.id,
              },
            };
          }),
        )
      : [];

  const [localTables, setLocalTables] = useState<Node<TableData>[]>(tablesFromStore);

  const tableIds = tablesFromStore
    .map((t) => t.id)
    .sort()
    .join(',');

  useEffect(() => {
    setLocalTables(tablesFromStore);
  }, [tableIds]);

  const tables = localTables;

  const addTable = useCallback(
    (position: { x: number; y: number }) => {
      if (erdStore.erdState.state !== 'loaded') {
        console.error('Database is not loaded');
        return;
      }

      // TODO: schema 매핑
      const firstSchema = erdStore.erdState.database.schemas[0];
      if (!firstSchema) {
        console.error('No schema found');
        return;
      }

      const tableCount = firstSchema.tables.length;
      erdStore.createTable(firstSchema.id, {
        id: `table_${Date.now()}`,
        name: `Table_${tableCount + 1}`,
        columns: [],
        indexes: [],
        constraints: [],
        relationships: [],
        tableOptions: '',
        extra: { position },
      });
    },
    [erdStore],
  );

  const onTablesChange = useCallback(
    (changes: NodeChange[]) => {
      setLocalTables((nds) => applyNodeChanges(changes, nds) as Node<TableData>[]);

      if (erdStore.erdState.state !== 'loaded') return;

      const { database } = erdStore.erdState;

      const positionChanges = changes.filter(
        (change) => change.type === 'position' && change.dragging === false && change.position,
      );

      positionChanges.forEach((change) => {
        if (change.type !== 'position' || !change.position) return;

        const table = tables.find((t) => t.id === change.id);
        if (!table) return;

        const schemaId: string = table.data.schemaId;
        const currentTable = database.schemas
          .find((s: Schema) => s.id === schemaId)
          ?.tables.find((t: Table) => t.id === change.id);

        if (currentTable) {
          const currentExtra = (currentTable.extra || {}) as TableExtra;
          const newExtra: TableExtra = {
            ...currentExtra,
            position: change.position,
          };
          erdStore.updateTableExtra(schemaId, change.id, newExtra);
        }
      });

      const removeChanges = changes.filter((change) => change.type === 'remove');
      removeChanges.forEach((change) => {
        const table = tables.find((t) => t.id === change.id);
        if (table?.data.schemaId) {
          erdStore.deleteTable(table.data.schemaId, change.id);
        }
      });
    },
    [erdStore, tables],
  );

  return {
    tables,
    addTable,
    onTablesChange,
  };
};
