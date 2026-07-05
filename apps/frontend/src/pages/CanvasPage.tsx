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
import { useNavigate, useParams } from '@tanstack/react-router';
import { useEffect } from 'react';
import axios from 'axios';
import { NotFoundPage } from './NotFoundPage';
import { useProject } from '@/features/project/hooks/useProject';

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
    data: { tables, memos, relationships },
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
      <div className="flex flex-1 overflow-hidden bg-schemafy-canvas">
        <Toolbar
          setActiveTool={setActiveTool}
          activeTool={activeTool}
          relationshipConfig={relationshipConfig}
          onRelationshipConfigChange={setRelationshipConfig}
        />

        <div className="relative flex-1 overflow-hidden">
          <div className="absolute right-6 top-6 z-10">
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
        <ShortcutPanel onClose={() => setIsShortcutPanelOpen(false)} />
      )}
      <RemoteCursors />
    </>
  );
});

export const CanvasPage = () => {
  const { projectId } = useParams({ from: '/project/$projectId' });
  const navigate = useNavigate();
  const { isProjectError, isLoadingProject, projectError } =
    useProject(projectId);
  const isForbidden =
    axios.isAxiosError(projectError) && projectError.response?.status === 403;

  useEffect(() => {
    if (!isProjectError || !isForbidden) return;

    void navigate({ to: '/workspace', replace: true });
  }, [isProjectError, isForbidden, navigate]);

  if (isLoadingProject || isForbidden) return null;
  if (isProjectError) return <NotFoundPage />;

  return (
    <SelectedSchemaProvider projectId={projectId}>
      <MemoProvider>
        <CanvasContent />
      </MemoProvider>
    </SelectedSchemaProvider>
  );
};
