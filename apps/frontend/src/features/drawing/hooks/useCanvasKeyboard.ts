import { useEffect, type RefObject } from 'react';
import type { Point } from '../types';

interface UseCanvasKeyboardParams {
  isChatOpen: boolean;
  isShortcutPanelOpen: boolean;
  mousePositionRef: RefObject<Point | null>;
  activeTool: string;
  openChatInput: (position: Point) => void;
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
  openChatInput,
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

        openChatInput({
          x: mousePositionRef.current.x + 16,
          y: mousePositionRef.current.y + 16,
        });
      }
    };

    window.addEventListener('keydown', handleKeyPress, true);
    return () => window.removeEventListener('keydown', handleKeyPress, true);
  }, [
    isChatOpen,
    isShortcutPanelOpen,
    activeTool,
    mousePositionRef,
    openChatInput,
    setActiveTool,
  ]);
};
