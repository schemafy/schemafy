import { useEffect, useCallback, useState } from 'react';
import { useReactFlow, type Viewport } from '@xyflow/react';
import { ErdStore } from '@/store/erd.store';

const LOCAL_STORAGE_KEY = 'schemafy-viewport';

type ViewportData = {
  selectedSchema: string;
  viewports: Record<string, Viewport>;
};

type ProjectViewports = Record<string, ViewportData>;

export const useViewport = () => {
  const erdStore = ErdStore.getInstance();
  const { setViewport, getViewport } = useReactFlow();
  const [selectedSchemaId, setSelectedSchemaId] = useState<string | null>(null);

  useEffect(() => {
    if (erdStore.erdState.state !== 'loaded') return;

    const projectId = erdStore.erdState.database.id;
    const schemas = erdStore.erdState.database.schemas;
    if (schemas.length === 0) return;

    try {
      const storageData = localStorage.getItem(LOCAL_STORAGE_KEY);
      if (storageData) {
        const allProjects = JSON.parse(storageData) as ProjectViewports;
        const projectData = allProjects[projectId];
        const storedSchemaId = projectData?.selectedSchema;
        const schemaExists = schemas.some((s) => s.id === storedSchemaId);

        if (storedSchemaId && schemaExists) {
          setSelectedSchemaId(storedSchemaId);
          return;
        }
      }
    } catch (e) {
      console.error('Failed to get stored selected schema', e);
    }

    setSelectedSchemaId(schemas[0].id);
  }, [erdStore.erdState]);

  useEffect(() => {
    if (erdStore.erdState.state !== 'loaded' || !selectedSchemaId) return;

    const projectId = erdStore.erdState.database.id;

    try {
      const storageData = localStorage.getItem(LOCAL_STORAGE_KEY);
      if (!storageData) return;

      const allProjects = JSON.parse(storageData) as ProjectViewports;
      const projectData = allProjects[projectId];
      if (!projectData) return;

      const viewport = projectData.viewports[selectedSchemaId];
      if (viewport) {
        setViewport(viewport, { duration: 0 });
      }
    } catch (e) {
      console.error('Failed to load viewport from localStorage', e);
    }
  }, [selectedSchemaId, erdStore.erdState, setViewport]);

  const handleMoveEnd = useCallback(() => {
    if (erdStore.erdState.state !== 'loaded' || !selectedSchemaId) return;

    const projectId = erdStore.erdState.database.id;
    const viewport = getViewport();

    try {
      const storageData = localStorage.getItem(LOCAL_STORAGE_KEY);
      let allProjects: ProjectViewports = {};

      if (storageData) {
        allProjects = JSON.parse(storageData);
      }

      if (!allProjects[projectId]) {
        allProjects[projectId] = {
          selectedSchema: selectedSchemaId,
          viewports: {},
        };
      }

      allProjects[projectId].selectedSchema = selectedSchemaId;
      allProjects[projectId].viewports[selectedSchemaId] = viewport;

      localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(allProjects));
    } catch (e) {
      console.error('Failed to save viewport to localStorage', e);
    }
  }, [selectedSchemaId, erdStore.erdState, getViewport]);

  const updateSelectedSchema = useCallback(
    (schemaId: string) => {
      if (erdStore.erdState.state !== 'loaded') return;

      const projectId = erdStore.erdState.database.id;
      setSelectedSchemaId(schemaId);

      try {
        const storageData = localStorage.getItem(LOCAL_STORAGE_KEY);
        let allProjects: ProjectViewports = {};

        if (storageData) {
          allProjects = JSON.parse(storageData);
        }

        if (!allProjects[projectId]) {
          allProjects[projectId] = {
            selectedSchema: schemaId,
            viewports: {},
          };
        } else {
          allProjects[projectId].selectedSchema = schemaId;
        }

        localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(allProjects));
      } catch (e) {
        console.error('Failed to update selected schema', e);
      }
    },
    [erdStore.erdState],
  );

  return {
    selectedSchemaId,
    updateSelectedSchema,
    handleMoveEnd,
  };
};
