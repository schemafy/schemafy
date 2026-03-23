import { observer } from 'mobx-react-lite';
import {
  Toolbar,
  RelationshipEditor,
  FloatingButtons,
  SchemaSelector,
  TempMemoPreview,
  useCanvasController,
  SelectedSchemaProvider,
  ReactFlowCanvas,
} from '@/features/drawing';
import { MemoProvider } from '@/features/memo/context';
import { ChatOverlay, ChatInput } from '@/components/Collaboration';

const CanvasContent = observer(() => {
  const {
    state: {
      relationshipConfig,
      activeTool,
      tempMemoPosition,
      chatInputPosition,
      selectedRelationship,
    },
    setter: { setRelationshipConfig, setActiveTool, setSelectedRelationship },
    data: { tables, memos, relationships },
    handlers: {
      onTableDragStop,
      onTablesDelete,
      onMemosChange,
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

          <ReactFlowCanvas
            tables={tables}
            memos={memos}
            relationships={relationships}
            activeTool={activeTool}
            onTableDragStop={onTableDragStop}
            onTablesDelete={onTablesDelete}
            onMemosChange={onMemosChange}
            onRelationshipsChange={onRelationshipsChange}
            onConnect={onConnect}
            onRelationshipClick={onRelationshipClick}
            onReconnect={onReconnect}
            onReconnectStart={onReconnectStart}
            onReconnectEnd={onReconnectEnd}
            handleMoveEnd={handleMoveEnd}
            handlePaneClick={handlePaneClick}
            handleMouseMove={handleMouseMove}
          />

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
        <FloatingButtons />
      </MemoProvider>
    </SelectedSchemaProvider>
  );
};
