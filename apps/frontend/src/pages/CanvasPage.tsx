import { useState, useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import { ulid } from 'ulid';
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
  SchemaSelector,
} from '@/features/drawing';
import { ErdStore } from '@/store/erd.store';

const NODE_TYPES = {
  table: TableNode,
};

const EDGE_TYPES = {
  customSmoothStep: CustomSmoothStepEdge,
};

const CanvasPageComponent = () => {
  const erdStore = ErdStore.getInstance();
  const [relationshipConfig, setRelationshipConfig] = useState<RelationshipConfig>({
    type: 'one-to-many',
    isNonIdentifying: false,
  });
  const [activeTool, setActiveTool] = useState<string>('pointer');
  const [mousePosition, setMousePosition] = useState<{
    x: number;
    y: number;
  } | null>(null);
  const { screenToFlowPosition } = useReactFlow();

  useEffect(() => {
    if (erdStore.erdState.state === 'idle') {
      const dbId = ulid();
      const schemaId = ulid();
      erdStore.load({
        id: dbId,
        schemas: [
          {
            id: schemaId,
            projectId: ulid(),
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
    deleteRelationship,
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
          <div className="absolute top-4 right-4 z-10">
            <SchemaSelector />
          </div>

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
              nodeColor={() => 'var(--color-schemafy-text)'}
              maskColor="var(--color-schemafy-bg-80)"
              style={{
                backgroundColor: 'var(--color-schemafy-bg-80)',
                boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
                borderRadius: '10px',
                overflow: 'hidden',
                position: 'absolute',
                bottom: '1rem',
                right: '1rem',
                margin: '0',
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
              onRelationshipDelete={deleteRelationship}
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
