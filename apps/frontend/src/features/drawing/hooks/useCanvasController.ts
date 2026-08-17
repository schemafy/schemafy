import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useKeyPress, useReactFlow } from '@xyflow/react';
import { useRelationships } from './useRelationships';
import { useTables } from './useTables';
import { useViewport } from './useViewport';
import { useCanvasKeyboard } from './useCanvasKeyboard';
import { useMemoContext } from '../../memo/hooks/useMemoStore';
import { useSelectedSchema } from '../contexts';
import { useSchemas } from './useSchemas';
import { useErdMutationSync } from './useErdMutationSync';
import { useUndoRedo } from './useUndoRedo';
import type { Point, RelationshipConfig } from '../types';
import { collaborationStore } from '@/store/collaboration.store';
import { useThrottledCallback } from '@/hooks/useThrottledCallback';
import { useChatInputAnchor } from '@/features/collaboration/hooks/useChatInputAnchor';

const CURSOR_THROTTLE_MS = 50;

export const useCanvasController = () => {
  const { projectId, selectedSchemaId } = useSelectedSchema();
  const { data: schemas } = useSchemas(projectId);
  useErdMutationSync(selectedSchemaId, projectId);
  const { screenToFlowPosition } = useReactFlow();

  const [relationshipConfig, setRelationshipConfig] =
    useState<RelationshipConfig>({
      type: 'one-to-many',
      isNonIdentifying: false,
    });
  const [activeTool, setActiveTool] = useState('pointer');
  const mousePositionRef = useRef<Point | null>(null);
  const [tempMemoPosition, setTempMemoPosition] = useState<{
    flow: Point;
    screen: Point;
  } | null>(null);
  const tempMemoPositionRef = useRef(tempMemoPosition);
  tempMemoPositionRef.current = tempMemoPosition;
  const chat = useChatInputAnchor();
  const [isShortcutPanelOpen, setIsShortcutPanelOpen] = useState(false);
  const isSpacePanKeyPressed = useKeyPress('Space');

  useEffect(() => {
    collaborationStore.connect(projectId);
    return () => {
      collaborationStore.disconnect();
    };
  }, [projectId]);

  useEffect(() => {
    const handleGlobalMouseMove = (e: MouseEvent) => {
      mousePositionRef.current = { x: e.clientX, y: e.clientY };
    };
    window.addEventListener('mousemove', handleGlobalMouseMove);
    return () => window.removeEventListener('mousemove', handleGlobalMouseMove);
  }, []);

  const { handleUndo, handleRedo } = useUndoRedo();

  useCanvasKeyboard({
    mousePositionRef,
    isChatOpen: chat.isOpen,
    isShortcutPanelOpen,
    activeTool,
    openChatInput: chat.open,
    setActiveTool,
    onUndo: handleUndo,
    onRedo: handleRedo,
  });

  const schemaIds = useMemo(() => schemas?.map((s) => s.id) ?? [], [schemas]);
  const { handleMoveEnd } = useViewport(schemaIds);
  const { tables, addTable, onNodeDragStop, onNodesDelete } = useTables();
  const { memos, onMemosChange, createMemo } = useMemoContext();

  const {
    relationships,
    selectedRelationship,
    onConnect,
    onRelationshipsChange,
    onRelationshipClick,
    updateRelationshipConfig,
    deleteRelationship,
    updateRelationshipName,
    setSelectedRelationship,
  } = useRelationships(relationshipConfig);

  const handleMemoCancel = useCallback(() => {
    setTempMemoPosition(null);
  }, []);

  const handleMemoCreate = useCallback(
    (content: string) => {
      if (tempMemoPositionRef.current) {
        createMemo(tempMemoPositionRef.current.flow, content.trim());
        setTempMemoPosition(null);
      }
    },
    [createMemo],
  );

  const handleChatSend = useCallback((message: string) => {
    collaborationStore.sendMessage(message);
  }, []);

  const handlePaneClick = useCallback(
    (e: React.MouseEvent) => {
      if (tempMemoPositionRef.current) {
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
      }
    },
    [activeTool, addTable, handleMemoCancel, screenToFlowPosition],
  );

  const sendCursorThrottled = useThrottledCallback(
    (clientX: number, clientY: number) => {
      const flowPosition = screenToFlowPosition({ x: clientX, y: clientY });
      collaborationStore.sendCursor(flowPosition.x, flowPosition.y);
    },
    CURSOR_THROTTLE_MS,
  );

  const handleMouseMove = useCallback(
    (e: React.MouseEvent) => {
      mousePositionRef.current = { x: e.clientX, y: e.clientY };
      if ((activeTool === 'hand' || isSpacePanKeyPressed) && e.buttons === 1)
        return;

      sendCursorThrottled(e.clientX, e.clientY);
    },
    [activeTool, isSpacePanKeyPressed, sendCursorThrottled],
  );

  return {
    state: {
      relationshipConfig,
      activeTool,
      tempMemoPosition,
      chatInputPosition: chat.position,
      isChatExiting: chat.isExiting,
      selectedRelationship,
      isShortcutPanelOpen,
    },
    setter: {
      setRelationshipConfig,
      setActiveTool,
      setTempMemoPosition,
      setSelectedRelationship,
      setIsShortcutPanelOpen,
    },
    data: {
      tables,
      memos,
      relationships,
    },
    handlers: {
      onTableDragStop: onNodeDragStop,
      onTablesDelete: onNodesDelete,
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
      closeChatInput: chat.close,
      handlePaneClick,
      handleMouseMove,
    },
  };
};
