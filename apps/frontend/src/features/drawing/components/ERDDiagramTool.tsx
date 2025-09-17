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
import { RelationshipMarker } from './RelationshipMarker';
import { RelationshipSelector } from './RelationshipSelector';
import { RELATIONSHIP_TYPES, type RelationshipType } from '../types';
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
  {
    id: 'table_2',
    type: 'table',
    position: { x: 500, y: 200 },
    data: { tableName: 'Posts' },
  },
];

export function ERDDiagramTool() {
  const [nodes, setNodes] = useState<Node[]>(initialNodes);
  const [edges, setEdges] = useState<Edge[]>([]);
  const [selectedEdge, setSelectedEdge] = useState<string | null>(null);
  const [currentRelationshipType, setCurrentRelationshipType] =
    useState<RelationshipType>('one-to-many');

  const onNodesChange = (changes: NodeChange[]) =>
    setNodes((nds) => applyNodeChanges(changes, nds) as Node[]);

  const onEdgesChange = (changes: EdgeChange[]) =>
    setEdges((eds) => applyEdgeChanges(changes, eds));

  const onConnect = (params: Connection) => {
    const relationshipConfig = RELATIONSHIP_TYPES[currentRelationshipType];

    const newEdge: Edge = {
      ...params,
      id: `edge_${Date.now()}`,
      type: 'smoothstep',
      style: relationshipConfig.style,
      markerStart: relationshipConfig.markerStart,
      markerEnd: relationshipConfig.markerEnd,
      label: relationshipConfig.label,
      labelStyle: {
        fontSize: 12,
        fontWeight: 'bold',
        color: relationshipConfig.style.stroke,
      },
      data: { relationshipType: currentRelationshipType },
    };

    setEdges((eds) => addEdge(newEdge, eds));
  };

  const onEdgeClick = (event: React.MouseEvent, edge: Edge) => {
    event.stopPropagation();
    setSelectedEdge(edge.id);
  };

  const changeRelationshipType = (
    edgeId: string,
    newType: RelationshipType,
  ) => {
    const config = RELATIONSHIP_TYPES[newType];
    setEdges((eds) =>
      eds.map((edge) =>
        edge.id === edgeId
          ? {
              ...edge,
              style: config.style,
              markerStart: config.markerStart,
              markerEnd: config.markerEnd,
              label: config.label,
              labelStyle: {
                fontSize: 12,
                fontWeight: 'bold',
                color: config.style.stroke,
              },
              data: { ...edge.data, relationshipType: newType },
            }
          : edge,
      ),
    );
    setSelectedEdge(null);
  };

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
    <>
      <RelationshipMarker />
      <div className="w-full h-screen flex flex-col">
        <div className="flex items-center gap-4 p-4 border-b border-schemafy-light-gray">
          <Button onClick={addTable}>Add Table</Button>
          <div className="flex items-center gap-2">
            <RelationshipSelector
              onSelect={setCurrentRelationshipType}
              currentType={currentRelationshipType}
            />
          </div>
        </div>

        <div className="flex-1 bg-schemafy-secondary relative">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onEdgeClick={onEdgeClick}
            nodeTypes={nodeTypes}
            connectionLineType={ConnectionLineType.SmoothStep}
            fitView
          />

          {selectedEdge && (
            <div className="absolute top-4 right-4 z-10">
              <RelationshipSelector
                onSelect={(type) => changeRelationshipType(selectedEdge, type)}
                currentType={
                  (edges.find((e) => e.id === selectedEdge)?.data
                    ?.relationshipType as RelationshipType) || 'one-to-many'
                }
              />
              <Button onClick={() => setSelectedEdge(null)}>Close</Button>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
