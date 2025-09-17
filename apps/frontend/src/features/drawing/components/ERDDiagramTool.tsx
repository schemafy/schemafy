import { useState } from 'react';
import {
  ReactFlow,
  applyNodeChanges,
  applyEdgeChanges,
  addEdge,
  ConnectionLineType,
} from '@xyflow/react';
import type {
  Node,
  Edge,
  NodeChange,
  EdgeChange,
  Connection,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { TableNode } from './TableNode';
import { Button } from '@/components';

const nodeTypes = {
  table: TableNode,
};

const initialNodes: Node[] = [
  {
    id: 'table_1',
    type: 'table',
    position: { x: 200, y: 200 },
    data: { tableName: 'Users' },
  },
];

export function ERDDiagramTool() {
  const [nodes, setNodes] = useState<Node[]>(initialNodes);
  const [edges, setEdges] = useState<Edge[]>([]);

  const onNodesChange = (changes: NodeChange[]) =>
    setNodes((nds) => applyNodeChanges(changes, nds) as Node[]);

  const onEdgesChange = (changes: EdgeChange[]) =>
    setEdges((eds) => applyEdgeChanges(changes, eds));

  const onConnect = (params: Connection) =>
    setEdges((eds) => addEdge(params, eds));

  const addTable = () => {
    const newNode: Node = {
      id: `table_${Date.now()}`,
      type: 'table',
      position: { x: Math.random() * 400, y: Math.random() * 300 },
      data: { tableName: `Table_${nodes.length + 1}` },
    };
    setNodes([...nodes, newNode]);
  };

  return (
    <div className="w-full h-screen flex flex-col">
      <div className="flex items-center gap-4 p-4 border-b border-schemafy-light-gray">
        <Button onClick={addTable}>Add Table</Button>
      </div>

      <div className="flex-1 bg-schemafy-secondary">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          nodeTypes={nodeTypes}
          connectionLineType={ConnectionLineType.SmoothStep}
          fitView
        ></ReactFlow>
      </div>
    </div>
  );
}
