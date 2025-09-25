import { useEffect, useState } from 'react';
import type { TableNodeData } from '../types';
import { applyNodeChanges, type Node, type NodeChange } from '@xyflow/react';

const INITIAL_NODES: Node[] = [
  {
    id: 'table_1',
    type: 'table',
    position: { x: 200, y: 200 },
    data: { tableName: 'Users' },
  },
  {
    id: 'table_2',
    type: 'table',
    position: { x: 500, y: 200 },
    data: { tableName: 'Posts' },
  },
];

export const useNodes = () => {
  const [nodes, setNodes] = useState<Node[]>(INITIAL_NODES);

  const updateNode = (nodeId: string, newData: Partial<TableNodeData>) => {
    setNodes((nds) =>
      nds.map((node) =>
        node.id === nodeId
          ? { ...node, data: { ...node.data, ...newData } }
          : node,
      ),
    );
  };

  useEffect(() => {
    setNodes((nds) =>
      nds.map((node) => ({
        ...node,
        data: { ...node.data, updateNode },
      })),
    );
  }, []);

  const addTable = (position: { x: number; y: number }) => {
    const newNode: Node = {
      id: `table_${Date.now()}`,
      type: 'table',
      position,
      data: {
        tableName: `Table_${nodes.length + 1}`,
        fields: [
          {
            id: `field_${Date.now()}`,
            name: 'id',
            type: 'INT',
            isPrimaryKey: true,
            isNotNull: true,
            isUnique: false,
          },
        ],
        updateNode,
      },
    };

    setNodes((prev) => [...prev, newNode]);
  };

  const onNodesChange = (changes: NodeChange[]) => {
    setNodes((nds) => applyNodeChanges(changes, nds) as Node[]);
  };

  return {
    nodes,
    updateNode,
    addTable,
    onNodesChange,
  };
};
