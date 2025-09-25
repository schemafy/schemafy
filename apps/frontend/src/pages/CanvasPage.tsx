import { useState } from 'react';
import {
  ReactFlow,
  ConnectionLineType,
  MiniMap,
  Controls,
  Background,
  BackgroundVariant,
  ConnectionMode,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import {
  useEdges,
  useNodes,
  TableNode,
  RelationshipMarker,
  Toolbar,
  EdgeSelector,
  type RelationshipConfig,
} from '@/features/drawing';

const NODE_TYPES = {
  table: TableNode,
};

export const CanvasPage = () => {
  const [relationshipConfig, setRelationshipConfig] =
    useState<RelationshipConfig>({
      type: 'one-to-many',
      isOptional: false,
    });

  const { nodes, addTable, onNodesChange } = useNodes();
  const {
    edges,
    selectedEdge,
    onConnect,
    onEdgesChange,
    onEdgeClick,
    changeRelationshipConfig,
    setSelectedEdge,
  } = useEdges(relationshipConfig);

  return (
    <>
      <RelationshipMarker />
      <div className="flex flex-1">
        <Toolbar
          onAddTable={() => addTable()}
          relationshipConfig={relationshipConfig}
          onRelationshipConfigChange={setRelationshipConfig}
        />

        <div className="flex-1 bg-schemafy-secondary relative">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onEdgeClick={onEdgeClick}
            nodeTypes={NODE_TYPES}
            connectionLineType={ConnectionLineType.SimpleBezier}
            proOptions={{ hideAttribution: true }}
            connectionMode={ConnectionMode.Loose}
            fitView
          >
            <MiniMap
              position="bottom-right"
              nodeColor={() => 'var(--color-schemafy-text)'}
              maskColor="var(--color-schemafy-bg-80)"
              style={{
                backgroundColor: 'var(--color-schemafy-bg-80)',
                boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
              }}
              zoomable
              pannable
            />
            <Controls position="top-left" />
            <Background variant={BackgroundVariant.Dots} />
          </ReactFlow>

          {selectedEdge && (
            <EdgeSelector
              selectedEdge={selectedEdge}
              edges={edges}
              onRelationshipChange={changeRelationshipConfig}
              onClose={() => setSelectedEdge(null)}
            />
          )}
        </div>
      </div>
    </>
  );
};
