import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useReactFlow } from '@xyflow/react';
import { useRelationships } from './useRelationships';
import { useTables } from './useTables';
import { useViewport } from './useViewport';
import { useCanvasKeyboard } from './useCanvasKeyboard';
import { useMemoContext } from '../../memo/hooks/useMemoStore';
import { useSelectedSchema } from '../contexts';
import { useSchemas } from './useSchemas';
import { useErdMutationSync } from './useErdMutationSync';
import type { RelationshipConfig, Point } from '../types';
import { collaborationStore } from '@/store/collaboration.store';

const CURSOR_THROTTLE_MS = 50;

export const useCanvasController = () => {
  const { projectId, selectedSchemaId } = useSelectedSchema();
  const { data: schemas } = useSchemas(projectId);
  useErdMutationSync(selectedSchemaId, projectId);
  const { screenToFlowPosition } = useReactFlow();
  const lastCursorSendTime = useRef<number>(0);

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
  const [chatInputPosition, setChatInputPosition] = useState<Point | null>(
    null,
  );

  useEffect(() => {
    collaborationStore.connect(projectId);
    return () => {
      collaborationStore.disconnect();
    };
  }, [projectId]);

  useCanvasKeyboard({
    chatInputPosition,
    mousePositionRef,
    activeTool,
    setChatInputPosition,
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
    setChatInputPosition(null);
  }, []);

  const handleChatCancel = useCallback(() => {
    setChatInputPosition(null);
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

  const handleMouseMove = useCallback(
    (e: React.MouseEvent) => {
      const position = { x: e.clientX, y: e.clientY };

      mousePositionRef.current = position;

      const now = Date.now();
      if (now - lastCursorSendTime.current >= CURSOR_THROTTLE_MS) {
        lastCursorSendTime.current = now;
        const flowPosition = screenToFlowPosition({
          x: position.x,
          y: position.y,
        });
        collaborationStore.sendCursor(flowPosition.x, flowPosition.y);
      }
    },
    [screenToFlowPosition],
  );

  return {
    state: {
      relationshipConfig,
      activeTool,
      tempMemoPosition,
      chatInputPosition,
      selectedRelationship,
    },
    setter: {
      setRelationshipConfig,
      setActiveTool,
      setTempMemoPosition,
      setChatInputPosition,
      setSelectedRelationship,
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
      handleChatCancel,
      handlePaneClick,
      handleMouseMove,
    },
  };
};
