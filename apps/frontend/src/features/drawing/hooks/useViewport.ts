import { useEffect, useCallback } from 'react';
import { useReactFlow, type Viewport } from '@xyflow/react';
import { useSelectedSchema } from '../contexts';
import { useLocalStorage } from './useLocalStorage';

const VIEWPORT_STORAGE_KEY = 'schemafy-viewport';

export const useViewport = () => {
  const { selectedSchemaId } = useSelectedSchema();
  const { setViewport, getViewport } = useReactFlow();
  const [viewportData, setViewportData] = useLocalStorage<
    Record<string, Viewport>
  >(VIEWPORT_STORAGE_KEY, {});

  useEffect(() => {
    if (!selectedSchemaId) return;

    const viewport = viewportData[selectedSchemaId];
    if (viewport) {
      setViewport(viewport, { duration: 0 });
    }
  }, [selectedSchemaId, viewportData, setViewport]);

  const handleMoveEnd = useCallback(() => {
    if (!selectedSchemaId) return;

    const viewport = getViewport();
    setViewportData((prev) => ({
      ...prev,
      [selectedSchemaId]: viewport,
    }));
  }, [selectedSchemaId, getViewport, setViewportData]);

  return {
    handleMoveEnd,
  };
};
