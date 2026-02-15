import {
  useState,
  useEffect,
  useRef,
  useCallback,
  type ReactNode,
} from 'react';
import { SelectedSchemaContext } from './useSelectedSchema';
import { useSchemas } from '../hooks/useSchemas';
import { useCreateSchema } from '../hooks/useSchemaMutations';

const getStorageKey = (projectId: string) => `selectedSchemaId_${projectId}`;

interface SelectedSchemaProviderProps {
  children: ReactNode;
  projectId: string;
}

export const SelectedSchemaProvider = ({
  children,
  projectId,
}: SelectedSchemaProviderProps) => {
  const storageKey = getStorageKey(projectId);
  const initializationAttempted = useRef(false);

  const [selectedSchemaId, setSelectedSchemaIdState] = useState<string | null>(
    () => {
      try {
        const stored = localStorage.getItem(storageKey);
        return stored;
      } catch {
        return null;
      }
    },
  );

  const { data: schemas, isLoading: isSchemasLoading } = useSchemas(projectId);
  const createSchemaMutation = useCreateSchema(projectId);
  const createMutationRef = useRef(createSchemaMutation);
  createMutationRef.current = createSchemaMutation;

  const setSelectedSchemaId = useCallback(
    (schemaId: string | null) => {
      setSelectedSchemaIdState(schemaId);
      try {
        if (schemaId) {
          localStorage.setItem(storageKey, schemaId);
        } else {
          localStorage.removeItem(storageKey);
        }
      } catch (error) {
        console.error('Failed to update localStorage:', error);
      }
    },
    [storageKey],
  );

  useEffect(() => {
    if (
      !selectedSchemaId &&
      !isSchemasLoading &&
      schemas &&
      !initializationAttempted.current
    ) {
      initializationAttempted.current = true;

      if (schemas.length === 0) {
        createMutationRef.current.mutate(
          {
            projectId,
            dbVendorName: 'mysql',
            name: 'schema1',
            charset: 'utf8mb4',
            collation: 'utf8mb4_general_ci',
          },
          {
            onSuccess: (response) => {
              if (response.data) {
                setSelectedSchemaId(response.data.id);
              }
            },
          },
        );
      } else {
        setSelectedSchemaId(schemas[0].id);
      }
    }
  }, [
    selectedSchemaId,
    schemas,
    isSchemasLoading,
    projectId,
    setSelectedSchemaId,
  ]);

  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === storageKey) {
        setSelectedSchemaIdState(e.newValue);
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [storageKey]);

  // TODO: 로딩 스피너
  if (!selectedSchemaId) {
    return null;
  }

  return (
    <SelectedSchemaContext.Provider
      value={{ projectId, selectedSchemaId, setSelectedSchemaId }}
    >
      {children}
    </SelectedSchemaContext.Provider>
  );
};
