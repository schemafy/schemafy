import { useEffect, useRef, useCallback } from 'react';
import { useReactFlow, type Viewport } from '@xyflow/react';
import { ErdStore } from '@/store/erd.store';

const SESSION_STORAGE_KEY = 'schemafy-viewport';

export const useViewport = () => {
  const erdStore = ErdStore.getInstance();
  const { setViewport, getViewport } = useReactFlow();
  const previousSchemaIdRef = useRef<string | null>(null);

  useEffect(() => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    if (!selectedSchemaId) return;

    const sessionData = sessionStorage.getItem(SESSION_STORAGE_KEY);
    if (sessionData) {
      try {
        const viewportData = JSON.parse(sessionData) as Record<
          string,
          Viewport
        >;
        const viewport = viewportData[selectedSchemaId];
        if (viewport) {
          setViewport(viewport, { duration: 0 });
          return;
        }
      } catch (e) {
        console.error('Failed to parse viewport from session storage', e);
      }
    }

    const selectedSchema = erdStore.selectedSchema;
    if (!selectedSchema) return;

    const extra = selectedSchema.extra as { viewport?: Viewport } | undefined;
    if (extra?.viewport) {
      setViewport(extra.viewport, { duration: 0 });
    }
  }, [erdStore.selectedSchemaId, erdStore.selectedSchema, setViewport]);

  const handleMoveEnd = useCallback(() => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    if (!selectedSchemaId) return;

    const viewport = getViewport();

    const sessionData = sessionStorage.getItem(SESSION_STORAGE_KEY);
    let viewportData: Record<string, Viewport> = {};
    if (sessionData) {
      try {
        viewportData = JSON.parse(sessionData);
      } catch (e) {
        console.error('Failed to parse viewport from session storage', e);
      }
    }

    viewportData[selectedSchemaId] = viewport;
    sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(viewportData));
  }, [erdStore.selectedSchemaId, getViewport]);

  const saveViewportToStore = useCallback(
    (schemaId: string) => {
      const sessionData = sessionStorage.getItem(SESSION_STORAGE_KEY);
      if (!sessionData) return;

      try {
        const viewportData = JSON.parse(sessionData) as Record<
          string,
          Viewport
        >;
        const viewport = viewportData[schemaId];
        if (!viewport) return;

        const schema =
          erdStore.erdState.state === 'loaded'
            ? erdStore.erdState.database.schemas.find((s) => s.id === schemaId)
            : null;

        if (schema) {
          const currentExtra =
            (schema.extra as { viewport?: Viewport } | undefined) || {};
          erdStore.updateSchemaExtra(schemaId, {
            ...currentExtra,
            viewport,
          });
        }
      } catch (e) {
        console.error('Failed to save viewport to erdStore', e);
      }
    },
    [erdStore],
  );

  useEffect(() => {
    const currentSchemaId = erdStore.selectedSchemaId;
    const previousSchemaId = previousSchemaIdRef.current;

    if (previousSchemaId && previousSchemaId !== currentSchemaId) {
      saveViewportToStore(previousSchemaId);
    }

    previousSchemaIdRef.current = currentSchemaId;
  }, [erdStore.selectedSchemaId, saveViewportToStore]);

  useEffect(() => {
    const handleBeforeUnload = () => {
      const selectedSchemaId = erdStore.selectedSchemaId;
      if (selectedSchemaId) {
        saveViewportToStore(selectedSchemaId);
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [erdStore, saveViewportToStore]);

  return {
    handleMoveEnd,
  };
};
