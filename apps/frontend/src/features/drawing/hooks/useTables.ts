import { useState, useEffect } from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import { toast } from 'sonner';
import { ErdStore } from '@/store';
import type { TableData, Point } from '../types';
import { transformTableToNode } from '../utils/tableHelpers';
import { generateUniqueName } from '../utils/nameGenerator';
import * as tableService from '../services/table.service';

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

  const addTable = async (position: Point) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = erdStore.selectedSchema;

    if (!selectedSchemaId || !selectedSchema) {
      toast.error('No schema selected');
      return;
    }

    const existingTableNames = selectedSchema.tables.map((table) => table.name);
    const tableName = generateUniqueName(existingTableNames, 'Table');

    try {
      await tableService.createTable(
        selectedSchemaId,
        tableName,
        '',
        undefined,
        JSON.stringify({ position }),
      );
    } catch (error) {
      toast.error('Failed to create table');
      console.error(error);
    }
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
      .forEach(async (change) => {
        if (change.type !== 'position' || !change.position) return;

        const table = nodes.find((t) => t.id === change.id);
        if (!table) return;

        try {
          await tableService.updateTableExtra(
            table.data.schemaId,
            change.id,
            {
              position: change.position,
            },
          );
        } catch (error) {
          console.error('Failed to update table position:', error);
        }
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
