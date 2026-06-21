import { Plus } from 'lucide-react';
import { Button } from '@/components';
import { SCHEMA_NAME_CONSTRAINTS } from '@/types';
import { useEmptySchemaState } from '../hooks/useEmptySchemaState';

interface EmptySchemaStateProps {
  projectId: string;
  onSchemaCreated: (schemaId: string) => void;
}

export const EmptySchemaState = ({
  projectId,
  onSchemaCreated,
}: EmptySchemaStateProps) => {
  const {
    schemaName,
    isValid,
    isPending,
    handleSchemaNameChange,
    handleSchemaNameKeyDown,
    handleCreateSchema,
  } = useEmptySchemaState({ projectId, onSchemaCreated });

  return (
    <div className="flex flex-1 w-full items-center justify-center bg-schemafy-secondary px-6">
      <div className="flex w-full max-w-sm flex-col items-center gap-5 rounded-lg bg-schemafy-bg p-6 shadow-lg">
        <div className="flex flex-col items-center gap-2 text-center">
          <h1 className="font-heading-md text-schemafy-text">
            No schemas yet
          </h1>
          <p className="font-body-sm text-schemafy-dark-gray">
            Create a schema to start editing this project.
          </p>
        </div>

        <div className="flex w-full flex-col gap-5">
          <div className="flex w-full flex-col gap-2">
            <input
              type="text"
              value={schemaName}
              onChange={handleSchemaNameChange}
              onKeyDown={handleSchemaNameKeyDown}
              placeholder="Schema name"
              maxLength={SCHEMA_NAME_CONSTRAINTS.MAX_LENGTH}
              disabled={isPending}
              className="w-full rounded-lg bg-schemafy-secondary p-3 font-body-sm text-schemafy-text focus:outline-none focus:ring-1 focus:ring-schemafy-button-bg disabled:text-schemafy-dark-gray"
              autoFocus
            />
            {!isValid && (
              <p className="font-caption-md text-schemafy-destructive">
                Schema name must be 1-20 characters
              </p>
            )}
          </div>

          <Button
            onClick={handleCreateSchema}
            disabled={!isValid || isPending}
            fullWidth
          >
            <Plus className="h-4 w-4" />
            {isPending ? 'Creating...' : 'Create Schema'}
          </Button>
        </div>
      </div>
    </div>
  );
};
