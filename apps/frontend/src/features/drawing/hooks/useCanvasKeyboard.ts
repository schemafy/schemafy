import { useEffect, type RefObject } from 'react';
import type { Point } from '../types';

interface UseCanvasKeyboardParams {
  chatInputPosition: Point | null;
  mousePositionRef: RefObject<Point | null>;
  activeTool: string;
  setChatInputPosition: (pos: Point | null) => void;
}

export const useCanvasKeyboard = ({
  chatInputPosition,
  mousePositionRef,
  activeTool,
  setChatInputPosition,
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

      if (e.key === '/' && !chatInputPosition && activeTool === 'pointer') {
        e.preventDefault();

        if (!mousePositionRef.current) return;

        setChatInputPosition({
          x: mousePositionRef.current.x,
          y: mousePositionRef.current.y,
        });
      }
    };

    window.addEventListener('keydown', handleKeyPress);
    return () => window.removeEventListener('keydown', handleKeyPress);
  }, [chatInputPosition, activeTool, setChatInputPosition, mousePositionRef]);
};
