import { useEffect } from 'react';
import type { Point } from '../types';

interface UseCanvasKeyboardParams {
  chatInputPosition: Point | null;
  mousePosition: Point | null;
  activeTool: string;
  setChatInputPosition: (pos: Point | null) => void;
}

export const useCanvasKeyboard = ({
  chatInputPosition,
  mousePosition,
  activeTool,
  setChatInputPosition,
}: UseCanvasKeyboardParams) => {
  useEffect(() => {
    const handleKeyPress = (e: KeyboardEvent) => {
      if (e.key === '/' && !chatInputPosition && activeTool === 'pointer') {
        e.preventDefault();

        if (!mousePosition) return;

        setChatInputPosition({
          x: mousePosition.x,
          y: mousePosition.y,
        });
      }
    };

    window.addEventListener('keydown', handleKeyPress);
    return () => window.removeEventListener('keydown', handleKeyPress);
  }, [chatInputPosition, mousePosition, activeTool, setChatInputPosition]);
};
