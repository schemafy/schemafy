import { observer } from 'mobx-react-lite';
import {
  ReactFlow,
  MiniMap,
  Background,
  BackgroundVariant,
  ConnectionMode,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import {
  TableNode,
  RelationshipMarker,
  Toolbar,
  RelationshipEditor,
  CustomControls,
  TablePreview,
  CustomSmoothStepEdge,
  CustomConnectionLine,
  FloatingButtons,
  SchemaSelector,
  TempMemoPreview,
  useCanvasController,
  SelectedSchemaProvider,
} from '@/features/drawing';
import { Memo, MemoPreview } from '@/features/memo/components';
import { MemoProvider } from '@/features/memo/context';
import { ChatOverlay, ChatInput } from '@/components/Collaboration';

const NODE_TYPES = {
  table: TableNode,
  memo: Memo,
};

const EDGE_TYPES = {
  customSmoothStep: CustomSmoothStepEdge,
};

const CanvasContent = observer(() => {
  const {
    state: {
      relationshipConfig,
      activeTool,
      mousePosition,
      tempMemoPosition,
      chatInputPosition,
      selectedRelationship,
    },
    setter: { setRelationshipConfig, setActiveTool, setSelectedRelationship },
    data: { nodes, relationships },
    handlers: {
      handleNodesChange,
      handleNodeDragStop,
      handleNodesDelete,
      onRelationshipsChange,
      handleMoveEnd,
      onConnect,
      onRelationshipClick,
      onReconnect,
      onReconnectStart,
      onReconnectEnd,
      updateRelationshipConfig,
      updateRelationshipName,
      deleteRelationship,
      handleMemoCancel,
      handleMemoCreate,
      handleChatSend,
      handleChatCancel,
      handlePaneClick,
      handleMouseMove,
    },
  } = useCanvasController();

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
              onNodeDragStop={handleNodeDragStop}
              onNodesDelete={handleNodesDelete}
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
              onRelationshipNameChange={updateRelationshipName}
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
});

export const CanvasPage = () => {
  const projectId = '06DS8JSJ7Y112MC87X0AB2CE8M';

  return (
    <SelectedSchemaProvider projectId={projectId}>
      <MemoProvider>
        <CanvasContent />
      </MemoProvider>
    </SelectedSchemaProvider>
  );
};
