import { useState, useEffect } from 'react';
import { observer } from 'mobx-react-lite';
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
  useRelationships,
  useTables,
  TableNode,
  RelationshipMarker,
  Toolbar,
  EdgeSelector,
  CustomControls,
  TablePreview,
  type RelationshipConfig,
  CustomSmoothStepEdge,
  FloatingButtons,
} from '@/features/drawing';
import { ErdStore } from '@/store/erd.store';

const NODE_TYPES = {
  table: TableNode,
};

const EDGE_TYPES = {
  customSmoothStep: CustomSmoothStepEdge,
};

const genId = (prefix: string) => `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;

const CanvasPageComponent = () => {
  const erdStore = ErdStore.getInstance();
  const [relationshipConfig, setRelationshipConfig] = useState<RelationshipConfig>({
    type: 'one-to-many',
    isDashed: false,
  });
  const [activeTool, setActiveTool] = useState<string>('pointer');
  const [mousePosition, setMousePosition] = useState<{
    x: number;
    y: number;
  } | null>(null);
  const { screenToFlowPosition } = useReactFlow();

  useEffect(() => {
    if (erdStore.erdState.state === 'idle') {
      const dbId = genId('db');
      const schemaId = genId('schema');
      erdStore.load({
        id: dbId,
        schemas: [
          {
            id: schemaId,
            projectId: genId('project'),
            dbVendorId: 'mysql',
            name: 'public',
            charset: 'utf8mb4',
            collation: 'utf8mb4_general_ci',
            vendorOption: '',
            createdAt: new Date(),
            updatedAt: new Date(),
            deletedAt: null,
            tables: [],
          },
        ],
      });
    }
  }, [erdStore]);

  const { tables, addTable, onTablesChange } = useTables();
  const {
    relationships,
    selectedRelationship,
    onConnect,
    onRelationshipsChange,
    onRelationshipClick,
    onReconnectStart,
    onReconnect,
    onReconnectEnd,
    changeRelationshipConfig,
    setSelectedRelationship,
  } = useRelationships(relationshipConfig);

  const handlePaneClick = (e: React.MouseEvent) => {
    if (activeTool === 'table') {
      const flowPosition = screenToFlowPosition({
        x: e.clientX,
        y: e.clientY,
      });
      addTable(flowPosition);
      setActiveTool('pointer');
      setMousePosition(null);
    }
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (activeTool === 'table') {
      setMousePosition({ x: e.clientX, y: e.clientY });
    } else {
      setMousePosition(null);
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
            nodes={tables}
            edges={relationships}
            onNodesChange={onTablesChange}
            onEdgesChange={onRelationshipsChange}
            onPaneClick={handlePaneClick}
            onPaneMouseMove={handleMouseMove}
            nodesDraggable={activeTool !== 'hand'}
            elementsSelectable={activeTool !== 'hand'}
            onConnect={onConnect}
            onEdgeClick={onRelationshipClick}
            onReconnect={onReconnect}
            onReconnectStart={onReconnectStart}
            onReconnectEnd={onReconnectEnd}
            nodeTypes={NODE_TYPES}
            edgeTypes={EDGE_TYPES}
            connectionLineType={ConnectionLineType.SmoothStep}
            proOptions={{ hideAttribution: true }}
            connectionMode={ConnectionMode.Loose}
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

            {activeTool === 'table' && <TablePreview mousePosition={mousePosition} />}
          </ReactFlow>

          {selectedRelationship && (
            <EdgeSelector
              selectedRelationship={selectedRelationship}
              relationships={relationships}
              onRelationshipChange={changeRelationshipConfig}
              onClose={() => setSelectedRelationship(null)}
            />
          )}
        </div>
      </div>
      <FloatingButtons />
    </>
  );
};

export const CanvasPage = observer(CanvasPageComponent);
