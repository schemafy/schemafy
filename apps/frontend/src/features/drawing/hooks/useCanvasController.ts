import { useState, useRef } from 'react';
import { useReactFlow } from '@xyflow/react';
import { useRelationships } from './useRelationships';
import { useTables } from './useTables';
import { useViewport } from './useViewport';
import { useCanvasInitialization } from './useCanvasInitialization';
import { useCanvasKeyboard } from './useCanvasKeyboard';
import { useCanvasNodes } from './useCanvasNodes';
import { useMemoContext } from '../../memo/hooks/useMemoStore';
import type { RelationshipConfig, Point } from '../types';
import { collaborationStore } from '@/store/collaboration.store';

const CURSOR_THROTTLE_MS = 100;

export const useCanvasController = () => {
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
  const { tables, addTable, onTablesChange, onNodeDragStop, onNodesDelete } =
    useTables();
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

  const { nodes, handleNodesChange, handleNodeDragStop, handleNodesDelete } =
    useCanvasNodes({
      tables,
      memos,
      onTablesChange,
      onMemosChange,
      onTableDragStop: onNodeDragStop,
      onTablesDelete: onNodesDelete,
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

  return {
    state: {
      relationshipConfig,
      activeTool,
      mousePosition,
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
      nodes,
      relationships,
    },
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
      changeRelationshipName,
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
