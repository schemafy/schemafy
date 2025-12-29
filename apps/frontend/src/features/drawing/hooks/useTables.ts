import { useState, useEffect } from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import { toast } from 'sonner';
import { ErdStore } from '@/store';
import type { TableData, Point } from '../types';
import { transformTableToNode } from '../utils/tableHelpers';
import { generateUniqueName } from '../utils/nameGenerator';
import * as tableService from '../services/table.service';
import { useViewportContext } from '../contexts';

export const useTables = () => {
  const erdStore = ErdStore.getInstance();
  const { selectedSchemaId } = useViewportContext();
  const [tables, setTables] = useState<Node<TableData>[]>([]);

  useEffect(() => {
    if (erdStore.erdState.state !== 'loaded' || !selectedSchemaId) {
      setTables([]);
      return;
    }

    const selectedSchema = erdStore.erdState.database.schemas.find(
      (s) => s.id === selectedSchemaId,
    );

    if (!selectedSchema) {
      setTables([]);
      return;
    }

    const newTables = selectedSchema.tables.map((table) =>
      transformTableToNode(table, selectedSchema.id),
    );
    setTables(newTables);
  }, [erdStore.erdState, selectedSchemaId]);

  const addTable = async (position: Point) => {
    if (erdStore.erdState.state !== 'loaded' || !selectedSchemaId) {
      toast.error('No schema selected');
      return;
    }

    const selectedSchema = erdStore.erdState.database.schemas.find(
      (s) => s.id === selectedSchemaId,
    );

    if (!selectedSchema) {
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
          await tableService.updateTableExtra(table.data.schemaId, change.id, {
            position: change.position,
          });
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
