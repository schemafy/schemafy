import { useEffect, useCallback, useRef } from 'react';
import { useReactFlow, type Viewport } from '@xyflow/react';
import { useSelectedSchema } from '../contexts';
import { useLocalStorage } from './useLocalStorage';

const getViewportStorageKey = (projectId: string) =>
  `schemafy-viewport_${projectId}`;

export const useViewport = (schemaIds: string[]) => {
  const { projectId, selectedSchemaId } = useSelectedSchema();
  const { setViewport, getViewport } = useReactFlow();
  const [viewportData, setViewportData] = useLocalStorage<
    Record<string, Viewport>
  >(getViewportStorageKey(projectId), {});

  const viewportDataRef = useRef(viewportData);
  viewportDataRef.current = viewportData;

  useEffect(() => {
    if (schemaIds.length === 0) return;

    const validIds = new Set(schemaIds);
    const staleIds = Object.keys(viewportDataRef.current).filter(
      (id) => !validIds.has(id),
    );

    if (staleIds.length > 0) {
      setViewportData((prev) => {
        const cleaned = { ...prev };
        staleIds.forEach((id) => delete cleaned[id]);
        return cleaned;
      });
    }
  }, [schemaIds, setViewportData]);

  useEffect(() => {
    const viewport = viewportDataRef.current[selectedSchemaId];
    if (viewport) {
      setViewport(viewport, { duration: 0 });
    }
  }, [selectedSchemaId, setViewport]);

  const handleMoveEnd = useCallback(() => {
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
