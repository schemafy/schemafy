import { useEffect, useRef, useState } from 'react';
import { useReactFlow } from '@xyflow/react';
import { useRelationships } from './useRelationships';
import { useTables } from './useTables';
import { useViewport } from './useViewport';
import { useCanvasKeyboard } from './useCanvasKeyboard';
import { useCanvasNodes } from './useCanvasNodes';
import { useMemoContext } from '../../memo/hooks/useMemoStore';
import { useSelectedSchema } from '../contexts';
import { useSchemas } from './useSchemas';
import { useErdMutationSync } from './useErdMutationSync';
import type { Point, RelationshipConfig } from '../types';
import { collaborationStore } from '@/store/collaboration.store';

const CURSOR_THROTTLE_MS = 100;

export const useCanvasController = () => {
  const { projectId, selectedSchemaId } = useSelectedSchema();
  const { data: schemas } = useSchemas(projectId);
  useErdMutationSync(selectedSchemaId, projectId);
  const { screenToFlowPosition } = useReactFlow();
  const lastCursorSendTime = useRef<number>(0);
  const lastChatPositionTime = useRef<number>(0);

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
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [isShortcutPanelOpen, setIsShortcutPanelOpen] = useState(false);

  useEffect(() => {
    collaborationStore.connect(projectId);
    return () => {
      collaborationStore.disconnect();
    };
  }, [projectId]);

  useEffect(() => {
    if (!isChatOpen) return;

    const handleWindowMouseMove = (e: MouseEvent) => {
      const now = Date.now();
      if (now - lastChatPositionTime.current < CURSOR_THROTTLE_MS) return;
      lastChatPositionTime.current = now;
      setChatInputPosition({ x: e.clientX + 16, y: e.clientY + 16 });
    };

    const handleMouseLeave = () => {
      setChatInputPosition(null);
      setIsChatOpen(false);
    };

    const handleClickOutside = (e: MouseEvent) => {
      const chatInputEl = document.querySelector('[data-chat-input]');
      if (!chatInputEl || !chatInputEl.contains(e.target as Node)) {
        setChatInputPosition(null);
        setIsChatOpen(false);
      }
    };

    window.addEventListener('mousemove', handleWindowMouseMove);
    document.addEventListener('mouseleave', handleMouseLeave);
    window.addEventListener('mousedown', handleClickOutside, true);

    return () => {
      window.removeEventListener('mousemove', handleWindowMouseMove);
      document.removeEventListener('mouseleave', handleMouseLeave);
      window.removeEventListener('mousedown', handleClickOutside, true);
    };
  }, [isChatOpen]);

  useCanvasKeyboard({
    isChatOpen,
    mousePosition,
    activeTool,
    setChatInputPosition,
    setIsChatOpen,
    setActiveTool,
  });

  const schemaIds = schemas?.map((s) => s.id) ?? [];
  const { handleMoveEnd } = useViewport(schemaIds);
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
    updateRelationshipName,
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
    setIsChatOpen(false);
  };

  const handleChatCancel = () => {
    setChatInputPosition(null);
    setIsChatOpen(false);
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
      isShortcutPanelOpen,
    },
    setter: {
      setRelationshipConfig,
      setActiveTool,
      setTempMemoPosition,
      setChatInputPosition,
      setSelectedRelationship,
      setIsShortcutPanelOpen,
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
