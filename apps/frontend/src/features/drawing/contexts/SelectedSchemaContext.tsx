import {
  useState,
  useEffect,
  useRef,
  useCallback,
  Suspense,
  type ReactNode,
} from 'react';
import { Loader2, TriangleAlert, RotateCcw } from 'lucide-react';
import { QueryErrorResetBoundary } from '@tanstack/react-query';
import { ErrorBoundary } from '@/components';
import { SelectedSchemaContext } from './useSelectedSchema';
import { useSchemas } from '../hooks/useSchemas';
import { useCreateSchema } from '../hooks/useSchemaMutations';

const getStorageKey = (projectId: string) => `selectedSchemaId_${projectId}`;

const SchemaLoading = () => (
  <div className="flex flex-1 w-full items-center justify-center bg-schemafy-bg">
    <div className="flex flex-col items-center gap-3">
      <Loader2 className="h-8 w-8 animate-spin text-schemafy-text" />
      <span className="text-sm text-schemafy-text">Loading schema...</span>
    </div>
  </div>
);

const SchemaError = ({ onRetry }: { onRetry: () => void }) => (
  <div className="flex flex-1 w-full items-center justify-center bg-schemafy-bg">
    <div className="flex flex-col items-center gap-3">
      <TriangleAlert className="h-8 w-8 text-red-500" />
      <span className="text-sm text-schemafy-text">
        Failed to load schema data.
      </span>
      <button
        onClick={onRetry}
        className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md bg-schemafy-secondary text-schemafy-text hover:opacity-80 transition-opacity"
      >
        <RotateCcw className="h-3.5 w-3.5" />
        Retry
      </button>
    </div>
  </div>
);

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

  const createInitialSchema = useCallback(() => {
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
  }, [projectId, setSelectedSchemaId]);

  useEffect(() => {
    if (selectedSchemaId && !isSchemasLoading && schemas) {
      const isValid = schemas.some((s) => s.id === selectedSchemaId);
      if (!isValid) {
        setSelectedSchemaId(schemas[0]?.id ?? null);
      }
    }
  }, [selectedSchemaId, schemas, isSchemasLoading, setSelectedSchemaId]);

  useEffect(() => {
    if (
      !selectedSchemaId &&
      !isSchemasLoading &&
      schemas &&
      !initializationAttempted.current
    ) {
      initializationAttempted.current = true;

      if (schemas.length === 0) {
        createInitialSchema();
      } else {
        setSelectedSchemaId(schemas[0].id);
      }
    }
  }, [
    selectedSchemaId,
    schemas,
    isSchemasLoading,
    createInitialSchema,
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

  if (!selectedSchemaId && createSchemaMutation.isError) {
    return <SchemaError onRetry={createInitialSchema} />;
  }

  if (!selectedSchemaId) {
    return <SchemaLoading />;
  }

  return (
    <SelectedSchemaContext.Provider
      value={{ projectId, selectedSchemaId, setSelectedSchemaId }}
    >
      <QueryErrorResetBoundary>
        {({ reset }) => (
          <ErrorBoundary
            onReset={reset}
            fallback={({ resetErrorBoundary }) => (
              <SchemaError onRetry={resetErrorBoundary} />
            )}
          >
            <Suspense fallback={<SchemaLoading />}>{children}</Suspense>
          </ErrorBoundary>
        )}
      </QueryErrorResetBoundary>
    </SelectedSchemaContext.Provider>
  );
};
