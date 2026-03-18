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
  'KeyP': 'pointer',
  'KeyH': 'hand',
  'KeyE': 'table',
  'KeyM': 'memo',
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

      if (TOOL_SHORTCUTS[e.code]) {
        e.preventDefault();
        setActiveTool(TOOL_SHORTCUTS[e.code]);
        return;
      }

      if (e.code === 'Slash' && !isChatOpen && (activeTool === 'pointer' || activeTool === 'hand')) {
        e.preventDefault();

        if (!mousePosition) return;

        setChatInputPosition({x: mousePosition.x + 16, y: mousePosition.y + 16});
        setIsChatOpen(true);
      }
    };

    window.addEventListener('keydown', handleKeyPress, true);
    return () => window.removeEventListener('keydown', handleKeyPress, true);
  }, [isChatOpen, mousePosition, activeTool, setChatInputPosition, setIsChatOpen, setActiveTool]);
};