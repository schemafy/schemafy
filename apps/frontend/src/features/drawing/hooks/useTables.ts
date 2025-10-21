import { useEffect, useState } from 'react';
import type { TableData } from '../types';
import { applyNodeChanges, type Node, type NodeChange } from '@xyflow/react';

const INITIAL_TABLES: Node[] = [];

export const useTables = () => {
  const [tables, setTables] = useState<Node[]>(INITIAL_TABLES);

  const updateTable = (tableId: string, newData: Partial<TableData>) => {
    setTables((tbls) => tbls.map((table) => (table.id === tableId ? { ...table, data: { ...table.data, ...newData } } : table)));
  };

  useEffect(() => {
    setTables((tbls) =>
      tbls.map((table) => ({
        ...table,
        data: { ...table.data, updateTable },
      })),
    );
  }, []);

  const addTable = (position: { x: number; y: number }) => {
    const newTable: Node = {
      id: `table_${Date.now()}`,
      type: 'table',
      position,
      data: {
        tableName: `Table_${tables.length + 1}`,
        updateTable,
      },
    };

    setTables((prev) => [...prev, newTable]);
  };

  const onTablesChange = (changes: NodeChange[]) => {
    setTables((tbls) => applyNodeChanges(changes, tbls) as Node[]);
  };

  return {
    tables,
    updateTable,
    addTable,
    onTablesChange,
  };
};
