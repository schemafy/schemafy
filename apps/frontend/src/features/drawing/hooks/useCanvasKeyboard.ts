import { useEffect } from 'react';
import type { Point } from '../types';

interface UseCanvasKeyboardParams {
  isChatOpen: boolean;
  mousePosition: Point | null;
  activeTool: string;
  setChatInputPosition: (pos: Point | null) => void;
  setIsChatOpen: (open: boolean) => void;
  setActiveTool: (tool: string) => void;
}

const TOOL_SHORTCUTS: Record<string, string> = {
  '1': 'pointer',
  '2': 'hand',
  '3': 'table',
  '4': 'memo',
};

export const useCanvasKeyboard = ({
  isChatOpen,
  mousePosition,
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

      if ((e.metaKey || e.ctrlKey) && TOOL_SHORTCUTS[e.key]) {
        e.preventDefault();
        setActiveTool(TOOL_SHORTCUTS[e.key]);
        return;
      }

      if (e.key === '/' && !isChatOpen && activeTool === 'pointer') {
        e.preventDefault();

        if (!mousePosition) return;

        setChatInputPosition({ x: mousePosition.x + 16, y: mousePosition.y + 16 });
        setIsChatOpen(true);
      }
    };

    window.addEventListener('keydown', handleKeyPress);
    return () => window.removeEventListener('keydown', handleKeyPress);
  }, [isChatOpen, mousePosition, activeTool, setChatInputPosition, setIsChatOpen, setActiveTool]);
};