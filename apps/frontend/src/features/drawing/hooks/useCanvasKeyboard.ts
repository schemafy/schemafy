import { useEffect, type RefObject } from 'react';
import type { Point } from '../types';

interface UseCanvasKeyboardParams {
  isChatOpen: boolean;
  isShortcutPanelOpen: boolean;
  mousePositionRef: RefObject<Point | null>;
  activeTool: string;
  setChatInputPosition: (pos: Point | null) => void;
  setIsChatOpen: (open: boolean) => void;
  setActiveTool: (tool: string) => void;
}

const TOOL_SHORTCUTS: Record<string, string> = {
  KeyP: 'pointer',
  KeyH: 'hand',
  KeyE: 'table',
  KeyM: 'memo',
};

export const useCanvasKeyboard = ({
  isChatOpen,
  isShortcutPanelOpen,
  mousePositionRef,
  activeTool,
  setChatInputPosition,
  setIsChatOpen,
  setActiveTool,
}: UseCanvasKeyboardParams) => {
  useEffect(() => {
    const handleKeyPress = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement;

      if (
        target.tagName === 'INPUT' ||
        target.tagName === 'TEXTAREA' ||
        target.isContentEditable
      )
        return;

      if (
        TOOL_SHORTCUTS[e.code] &&
        !isChatOpen &&
        !isShortcutPanelOpen &&
        !e.metaKey &&
        !e.ctrlKey &&
        !e.altKey &&
        !e.shiftKey
      ) {
        e.preventDefault();
        setActiveTool(TOOL_SHORTCUTS[e.code]);
        return;
      }

      if (
        e.key === '/' &&
        !isChatOpen &&
        (activeTool === 'pointer' || activeTool === 'hand')
      ) {
        e.preventDefault();

        if (!mousePositionRef.current) return;

        setChatInputPosition({
          x: mousePositionRef.current.x,
          y: mousePositionRef.current.y,
        });
        setIsChatOpen(true);
      }
    };

    window.addEventListener('keydown', handleKeyPress, true);
    return () => window.removeEventListener('keydown', handleKeyPress, true);
  }, [
    isChatOpen,
    isShortcutPanelOpen,

    activeTool,
    setChatInputPosition,
    mousePositionRef,
    setIsChatOpen,
    setActiveTool,
  ]);
};
