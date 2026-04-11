import {
  FloatingButtons,
  ReactFlowCanvas,
  RelationshipEditor,
  SchemaSelector,
  SelectedSchemaProvider,
  ShortcutPanel,
  TempMemoPreview,
  Toolbar,
  useCanvasController,
} from '@/features/drawing';
import { MemoProvider } from '@/features/memo/context';
import { ChatInput, RemoteCursors } from '@/features/collaboration/components';
import { observer } from 'mobx-react-lite';

const CanvasContent = observer(() => {
  const {
    state: {
      relationshipConfig,
      activeTool,
      tempMemoPosition,
      chatInputPosition,
      isChatExiting,
      selectedRelationship,
      isShortcutPanelOpen,
    },
    setter: {
      setRelationshipConfig,
      setActiveTool,
      setSelectedRelationship,
      setIsShortcutPanelOpen,
    },
    data: {tables, memos, relationships},
    handlers: {
      onTableDragStop,
      onTablesDelete,
      onMemosChange,
      onRelationshipsChange,
      handleMoveEnd,
      onConnect,
      onRelationshipClick,
      updateRelationshipConfig,
      updateRelationshipName,
      deleteRelationship,
      handleMemoCancel,
      handleMemoCreate,
      handleChatSend,
      closeChatInput,
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
            <SchemaSelector/>
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
              isExiting={isChatExiting}
              onSend={handleChatSend}
              onCancel={closeChatInput}
            />
          )}
        </div>
      </div>
      <FloatingButtons
        isShortcutPanelOpen={isShortcutPanelOpen}
        onHelpClick={() => setIsShortcutPanelOpen((prev) => !prev)}
      />
      {isShortcutPanelOpen && (
        <ShortcutPanel onClose={() => setIsShortcutPanelOpen(false)}/>
      )}
      <RemoteCursors/>
    </>
  );
});

export const CanvasPage = () => {
  const projectId = '06EF3RWHVWADZEMACHXTSGA3Q0';

  return (
    <SelectedSchemaProvider projectId={projectId}>
      <MemoProvider>
        <CanvasContent/>
      </MemoProvider>
    </SelectedSchemaProvider>
  );
};
