import { useState, useRef } from 'react';
import { observer } from 'mobx-react-lite';
import {
  ReactFlow,
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
  useViewport,
  useCanvasInitialization,
  useCanvasKeyboard,
  useCanvasNodes,
  TableNode,
  RelationshipMarker,
  Toolbar,
  RelationshipEditor,
  CustomControls,
  TablePreview,
  type RelationshipConfig,
  type Point,
  CustomSmoothStepEdge,
  CustomConnectionLine,
  FloatingButtons,
  SchemaSelector,
  MemoPreview,
  Memo,
  TempMemoPreview,
  MemoProvider,
  useMemoContext,
} from '@/features/drawing';
import { ChatOverlay, ChatInput } from '@/components/Collaboration';
import { collaborationStore } from '@/store/collaboration.store';

const NODE_TYPES = {
  table: TableNode,
  memo: Memo,
};

const EDGE_TYPES = {
  customSmoothStep: CustomSmoothStepEdge,
};

const CURSOR_THROTTLE_MS = 100;

const CanvasContent = () => {
  const { screenToFlowPosition } = useReactFlow();
  const lastCursorSendTime = useRef<number>(0);

  const [relationshipConfig, setRelationshipConfig] =
    useState<RelationshipConfig>({
      type: 'one-to-many',
      isNonIdentifying: false,
    });
  const [activeTool, setActiveTool] = useState('pointer');
  const [mousePosition, setMousePosition] = useState<Point | null>(null);
  const [tempMemoPosition, setTempMemoPosition] = useState<{
    flow: Point;
    screen: Point;
  } | null>(null);
  const [chatInputPosition, setChatInputPosition] = useState<Point | null>(
    null,
  );

  useCanvasInitialization();

  useCanvasKeyboard({
    chatInputPosition,
    mousePosition,
    activeTool,
    setChatInputPosition,
  });

  const { handleMoveEnd } = useViewport();
  const { tables, addTable, onTablesChange } = useTables();
  const { memos, onMemosChange, createMemo } = useMemoContext();

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

  const { nodes, handleNodesChange } = useCanvasNodes({
    tables,
    memos,
    onTablesChange,
    onMemosChange,
  });

  const handleMemoCancel = () => {
    setTempMemoPosition(null);
  };

  const handleMemoCreate = (content: string) => {
    if (tempMemoPosition) {
      createMemo(tempMemoPosition.flow, content.trim());
      setTempMemoPosition(null);
    }
  };

  const handleChatSend = (message: string) => {
    collaborationStore.sendMessage(message);
    setChatInputPosition(null);
  };

  const handleChatCancel = () => {
    setChatInputPosition(null);
  };

  const handlePaneClick = (e: React.MouseEvent) => {
    if (tempMemoPosition) {
      handleMemoCancel();
      return;
    }

    if (activeTool !== 'table' && activeTool !== 'memo') return;

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
    const position = { x: e.clientX, y: e.clientY };
    setMousePosition(position);

    const now = Date.now();
    if (now - lastCursorSendTime.current >= CURSOR_THROTTLE_MS) {
      lastCursorSendTime.current = now;
      const flowPosition = screenToFlowPosition({
        x: position.x,
        y: position.y,
      });
      collaborationStore.sendCursor(flowPosition.x, flowPosition.y);
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

          <div
            style={{
              cursor: activeTool === 'hand' ? 'grab' : 'default',
            }}
            className="w-full h-full"
          >
            <ReactFlow
              nodes={nodes}
              edges={relationships}
              onNodesChange={handleNodesChange}
              onEdgesChange={onRelationshipsChange}
              onPaneClick={handlePaneClick}
              onPaneMouseMove={handleMouseMove}
              onMoveEnd={handleMoveEnd}
              nodesDraggable={activeTool !== 'hand'}
              elementsSelectable={activeTool !== 'hand'}
              panOnDrag={activeTool === 'hand'}
              onConnect={onConnect}
              onEdgeClick={onRelationshipClick}
              onReconnect={onReconnect}
              onReconnectStart={onReconnectStart}
              onReconnectEnd={onReconnectEnd}
              nodeTypes={NODE_TYPES}
              edgeTypes={EDGE_TYPES}
              connectionLineComponent={CustomConnectionLine}
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

              {activeTool === 'table' && (
                <TablePreview mousePosition={mousePosition} />
              )}
              {activeTool === 'memo' && (
                <MemoPreview mousePosition={mousePosition} />
              )}
            </ReactFlow>
          </div>

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

          {chatInputPosition && (
            <ChatInput
              position={chatInputPosition}
              onSend={handleChatSend}
              onCancel={handleChatCancel}
            />
          )}
        </div>
      </div>
      <FloatingButtons />
      <ChatOverlay />
    </>
  );
};

const CanvasPageComponent = () => {
  return (
    <MemoProvider>
      <CanvasContent />
    </MemoProvider>
  );
};

export const CanvasPage = observer(CanvasPageComponent);
