import { useState, useEffect } from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import { toast } from 'sonner';
import { ErdStore } from '@/store';
import type { TableData, Point } from '../types';
import { ulid } from 'ulid';
import { transformTableToNode } from '../utils/tableHelpers';
import { generateUniqueName } from '../utils/nameGenerator';

export const useTables = () => {
  const erdStore = ErdStore.getInstance();

  const getTablesFromStore = (): Node<TableData>[] => {
    const selectedSchema = erdStore.selectedSchema;

    if (!selectedSchema) return [];

    return selectedSchema.tables.map((table) =>
      transformTableToNode(table, selectedSchema.id),
    );
  };

  const [tables, setTables] = useState<Node<TableData>[]>(getTablesFromStore());

  useEffect(() => {
    setTables(getTablesFromStore());
  }, [erdStore.erdState, erdStore.selectedSchemaId]);

  const addTable = (position: Point) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = erdStore.selectedSchema;

    if (!selectedSchemaId || !selectedSchema) {
      toast.error('No schema selected');
      return;
    }

    const existingTableNames = selectedSchema.tables.map((table) => table.name);

    erdStore.createTable(selectedSchemaId, {
      id: ulid(),
      name: generateUniqueName(existingTableNames, 'Table'),
      columns: [],
      indexes: [],
      constraints: [],
      relationships: [],
      tableOptions: '',
      extra: { position },
      isAffected: false,
    });
  };

  const handlePositionChanges = (
    changes: NodeChange[],
    nodes: Node<TableData>[],
  ) => {
    changes
      .filter(
        (change) =>
          change.type === 'position' &&
          change.dragging === false &&
          change.position,
      )
      .forEach((change) => {
        if (change.type !== 'position' || !change.position) return;

        const table = nodes.find((t) => t.id === change.id);
        if (!table) return;

        erdStore.updateTableExtra(table.data.schemaId, change.id, {
          position: change.position,
        });
      });
  };

  const handleRemoveChanges = (
    changes: NodeChange[],
    nodes: Node<TableData>[],
  ) => {
    changes
      .filter((change) => change.type === 'remove')
      .forEach((change) => {
        const table = nodes.find((t) => t.id === change.id);
        if (table?.data.schemaId) {
          erdStore.deleteTable(table.data.schemaId, change.id);
        }
      });
  };

  const onTablesChange = (changes: NodeChange[]) => {
    setTables((nds) => {
      const updatedTables = applyNodeChanges(changes, nds) as Node<TableData>[];
      handlePositionChanges(changes, nds);
      handleRemoveChanges(changes, nds);
      return updatedTables;
    });
  };

  return {
    tables,
    addTable,
    onTablesChange,
  };
};
