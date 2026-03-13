import { useState, useEffect } from 'react';
import type { Point } from '../types';
import { TablePreview } from './TablePreview';
import { MemoPreview } from '@/features/memo/components';

interface CanvasPreviewLayerProps {
  activeTool: string;
}

export const CanvasPreviewLayer = ({ activeTool }: CanvasPreviewLayerProps) => {
  const isPreviewActive = activeTool === 'table' || activeTool === 'memo';
  const [mousePosition, setMousePosition] = useState<Point | null>(null);

  useEffect(() => {
    if (!isPreviewActive) {
      setMousePosition(null);
      return;
    }

    const handleMouseMove = (e: MouseEvent) => {
      setMousePosition({ x: e.clientX, y: e.clientY });
    };

    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
  }, [isPreviewActive]);

  if (!isPreviewActive) return null;

  return (
    <>
      {activeTool === 'table' && <TablePreview mousePosition={mousePosition} />}
      {activeTool === 'memo' && <MemoPreview mousePosition={mousePosition} />}
    </>
  );
};
