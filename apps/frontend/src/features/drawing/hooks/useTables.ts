import { useState, useEffect } from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import { ErdStore } from '@/store';
import type { TableData } from '../types';
import { ulid } from 'ulid';
import { transformTableToNode } from '../utils/tableHelpers';

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
