import {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
  Suspense,
  type ReactNode,
} from 'react';
import { TriangleAlert, RotateCcw } from 'lucide-react';
import { QueryErrorResetBoundary } from '@tanstack/react-query';
import { ErrorBoundary, LoadingState } from '@/components';
import { SelectedSchemaContext } from './useSelectedSchema';
import { useSchemas } from '../hooks/useSchemas';
import { useCreateSchema } from '../hooks/useSchemaMutations';

const getStorageKey = (projectId: string) => `selectedSchemaId_${projectId}`;

const SchemaLoading = () => (
  <LoadingState label="Loading schema..." spinnerClassName="h-8 w-8" />
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

  const {
    data: schemas,
    isLoading: isSchemasLoading,
    isError: isSchemasError,
    refetch: refetchSchemas,
  } = useSchemas(projectId);
  const { mutate: createSchema, isError: isCreateSchemaError } =
    useCreateSchema(projectId);

  const setSelectedSchemaId = useCallback(
    (schemaId: string | null) => {
      setSelectedSchemaIdState(schemaId);
      if (schemaId) {
        localStorage.setItem(storageKey, schemaId);
      } else {
        localStorage.removeItem(storageKey);
      }
    },
    [storageKey],
  );

  const createInitialSchema = useCallback(() => {
    createSchema(
      {
        projectId,
        dbVendorName: 'mysql',
        name: 'schema1',
        charset: 'utf8mb4',
        collation: 'utf8mb4_general_ci',
      },
      {
        onSuccess: (response) => {
          if (response?.data) {
            setSelectedSchemaId(response.data.id);
          }
        },
      },
    );
  }, [createSchema, projectId, setSelectedSchemaId]);

  const activeSchemaId = useMemo(() => {
    if (!schemas || schemas.length === 0) return null;

    const selectedSchema = schemas.find(
      (schema) => schema.id === selectedSchemaId,
    );
    return selectedSchema?.id ?? schemas[0].id;
  }, [schemas, selectedSchemaId]);

  useEffect(() => {
    if (isSchemasLoading || !schemas) return;

    if (schemas.length === 0) {
      if (selectedSchemaId !== null) {
        setSelectedSchemaId(null);
      }
      if (!initializationAttempted.current) {
        initializationAttempted.current = true;
        createInitialSchema();
      }
      return;
    }

    initializationAttempted.current = false;

    if (activeSchemaId && selectedSchemaId !== activeSchemaId) {
      setSelectedSchemaId(activeSchemaId);
    }
  }, [
    activeSchemaId,
    selectedSchemaId,
    schemas,
    isSchemasLoading,
    createInitialSchema,
    setSelectedSchemaId,
  ]);

  const contextValue = useMemo(
    () => ({
      projectId,
      selectedSchemaId: activeSchemaId as string,
      setSelectedSchemaId,
    }),
    [projectId, activeSchemaId, setSelectedSchemaId],
  );

  if (!activeSchemaId && (isSchemasError || isCreateSchemaError)) {
    return (
      <SchemaError
        onRetry={isSchemasError ? () => refetchSchemas() : createInitialSchema}
      />
    );
  }

  if (isSchemasLoading || !activeSchemaId) {
    return <SchemaLoading />;
  }

  return (
    <SelectedSchemaContext.Provider value={contextValue}>
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
