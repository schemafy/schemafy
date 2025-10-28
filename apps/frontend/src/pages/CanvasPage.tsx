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
  type NodeChange,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import {
  useRelationships,
  useTables,
  useMemos,
  TableNode,
  RelationshipMarker,
  Toolbar,
  RelationshipEditor,
  CustomControls,
  TablePreview,
  type RelationshipConfig,
  CustomSmoothStepEdge,
  FloatingButtons,
  SchemaSelector,
  MemoPrivew,
  Memo,
  TempMemoPreview,
} from '@/features/drawing';
import { ErdStore } from '@/store/erd.store';

const NODE_TYPES = {
  table: TableNode,
  memo: Memo,
};

const EDGE_TYPES = {
  customSmoothStep: CustomSmoothStepEdge,
};

const CanvasPageComponent = () => {
  const erdStore = ErdStore.getInstance();
  const { screenToFlowPosition } = useReactFlow();

  const [relationshipConfig, setRelationshipConfig] = useState<RelationshipConfig>({
    type: 'one-to-many',
    isNonIdentifying: false,
  });
  const [activeTool, setActiveTool] = useState('pointer');
  const [mousePosition, setMousePosition] = useState<{ x: number; y: number } | null>(null);
  const [tempMemoPosition, setTempMemoPosition] = useState<{
    flow: { x: number; y: number };
    screen: { x: number; y: number };
  } | null>(null);

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
            name: 'schema1',
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
  const { memos, addMemo, onMemosChange } = useMemos();
  const {
    relationships,
    selectedRelationship,
    onConnect,
    onRelationshipsChange,
    onRelationshipClick,
    onReconnectStart,
    onReconnect,
    onReconnectEnd,
    updateRelationshipConfig,
    deleteRelationship,
    changeRelationshipName,
    setSelectedRelationship,
  } = useRelationships(relationshipConfig);

  const nodes = [...tables, ...memos];

  const handleNodesChange = (changes: NodeChange[]) => {
    const tableChanges: NodeChange[] = [];
    const memoChanges: NodeChange[] = [];

    changes.forEach((change) => {
      if (!('id' in change)) return;

      const isTable = tables.some((t) => t.id === change.id);
      const isMemo = memos.some((m) => m.id === change.id);

      if (isTable) {
        tableChanges.push(change);
      } else if (isMemo) {
        memoChanges.push(change);
      }
    });

    if (tableChanges.length > 0) {
      onTablesChange(tableChanges);
    }

    if (memoChanges.length > 0) {
      onMemosChange(memoChanges);
    }
  };

  const handleMemoCancel = () => {
    setTempMemoPosition(null);
  };

  const handleMemoCreate = (content: string) => {
    if (tempMemoPosition) {
      addMemo(tempMemoPosition.flow, content.trim());
      setTempMemoPosition(null);
    }
  };

  const handlePaneClick = (e: React.MouseEvent) => {
    if (tempMemoPosition) {
      handleMemoCancel();
      return;
    }

    if (activeTool !== 'table' && activeTool !== 'memo') {
      return;
    }

    const flowPosition = screenToFlowPosition({
      x: e.clientX,
      y: e.clientY,
    });

    if (activeTool === 'table') {
      addTable(flowPosition);
      setActiveTool('pointer');
      setMousePosition(null);
    } else if (activeTool === 'memo') {
      const target = e.currentTarget as HTMLElement;
      const rect = target.getBoundingClientRect();
      setTempMemoPosition({
        flow: flowPosition,
        screen: {
          x: e.clientX - rect.left,
          y: e.clientY - rect.top,
        },
      });
      setActiveTool('pointer');
      setMousePosition(null);
    }
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (activeTool === 'table' || activeTool === 'memo') {
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
            nodes={nodes}
            edges={relationships}
            onNodesChange={handleNodesChange}
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
            fitView={false}
            minZoom={0.1}
            maxZoom={4}
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
            {activeTool === 'memo' && <MemoPrivew mousePosition={mousePosition} />}
          </ReactFlow>

          {selectedRelationship && (
            <RelationshipEditor
              selectedRelationship={selectedRelationship}
              relationships={relationships}
              onRelationshipChange={updateRelationshipConfig}
              onRelationshipNameChange={changeRelationshipName}
              onRelationshipDelete={deleteRelationship}
              onClose={() => setSelectedRelationship(null)}
            />
          )}

          {tempMemoPosition && (
            <TempMemoPreview
              position={tempMemoPosition.screen}
              onConfirm={handleMemoCreate}
              onCancel={handleMemoCancel}
            />
          )}
        </div>
      </div>
      <FloatingButtons />
    </>
  );
};

export const CanvasPage = observer(CanvasPageComponent);
