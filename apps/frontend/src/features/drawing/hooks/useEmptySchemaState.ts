import { useState } from 'react';
import { toast } from 'sonner';
import { useCreateSchema } from './useSchemaMutations';
import { validateSchemaName } from '../utils/validateSchemaName';

interface UseEmptySchemaStateParams {
  projectId: string;
  onSchemaCreated: (schemaId: string) => void;
}

export const useEmptySchemaState = ({
  projectId,
  onSchemaCreated,
}: UseEmptySchemaStateParams) => {
  const [schemaName, setSchemaName] = useState('schema1');
  const createSchemaMutation = useCreateSchema(projectId);
  const trimmedName = schemaName.trim();
  const isValid = validateSchemaName(trimmedName);

  const handleCreateSchema = () => {
    if (!isValid || createSchemaMutation.isPending) return;

    createSchemaMutation.mutate(
      {
        projectId,
        dbVendorName: 'mysql',
        name: trimmedName,
        charset: 'utf8mb4',
        collation: 'utf8mb4_general_ci',
      },
      {
        onSuccess: (response) => {
          onSchemaCreated(response.data.id);
        },
        onError: () => {
          toast.error('Failed to create schema');
        },
      },
    );
  };

  const handleSchemaNameChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    setSchemaName(event.target.value);
  };

  const handleSchemaNameKeyDown = (
    event: React.KeyboardEvent<HTMLInputElement>,
  ) => {
    if (event.key === 'Enter') {
      handleCreateSchema();
    }
  };

  return {
    schemaName,
    isValid,
    isPending: createSchemaMutation.isPending,
    handleSchemaNameChange,
    handleSchemaNameKeyDown,
    handleCreateSchema,
  };
};
