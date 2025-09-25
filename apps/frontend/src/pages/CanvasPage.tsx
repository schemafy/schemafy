import { useState } from 'react';
import {
  ReactFlow,
  ConnectionLineType,
  MiniMap,
  Background,
  BackgroundVariant,
  ConnectionMode,
  useReactFlow,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import {
  useEdges,
  useNodes,
  TableNode,
  RelationshipMarker,
  Toolbar,
  EdgeSelector,
  CustomControls,
  TablePreview,
  type RelationshipConfig,
} from '@/features/drawing';

const NODE_TYPES = {
  table: TableNode,
};

export const CanvasPage = () => {
  const [relationshipConfig, setRelationshipConfig] =
    useState<RelationshipConfig>({
      type: 'one-to-many',
      isDashed: false,
    });
  const [activeTool, setActiveTool] = useState<string>('pointer');
  const [mousePosition, setMousePosition] = useState({ x: 0, y: 0 });
  const { screenToFlowPosition } = useReactFlow();

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

  const handlePaneClick = (e: React.MouseEvent) => {
    if (activeTool === 'table') {
      const flowPosition = screenToFlowPosition({
        x: e.clientX,
        y: e.clientY,
      });
      addTable(flowPosition);
      setActiveTool('pointer');
    }
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (activeTool === 'table') {
      setMousePosition({ x: e.pageX, y: e.pageY });
    }
  };

  return (
    <>
      <RelationshipMarker />
      <div className="flex flex-1">
        <Toolbar
          setActiveTool={setActiveTool}
          activeTool={activeTool}
          relationshipConfig={relationshipConfig}
          onRelationshipConfigChange={setRelationshipConfig}
        />

        <div className="flex-1 bg-schemafy-secondary relative">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onPaneClick={handlePaneClick}
            onPaneMouseMove={handleMouseMove}
            nodesDraggable={activeTool !== 'hand'}
            elementsSelectable={activeTool !== 'hand'}
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
                borderRadius: '10px',
                overflow: 'hidden',
              }}
              zoomable
              pannable
            />
            <CustomControls />
            <Background variant={BackgroundVariant.Dots} />

            {activeTool === 'table' && (
              <TablePreview mousePosition={mousePosition} />
            )}
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
