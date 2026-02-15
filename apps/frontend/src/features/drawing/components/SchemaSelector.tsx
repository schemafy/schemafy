import { useState } from 'react';
import { toast } from 'sonner';
import { Button } from '@/components';
import { ChevronDown, ChevronUp } from 'lucide-react';
import { SchemaInput } from './SchemaInput';
import { SchemaListItem } from './SchemaListItem';
import {
  useSchemaEditor,
  useSchemas,
  useCreateSchema,
  useChangeSchemaName,
  useDeleteSchema,
} from '../hooks';
import { useSelectedSchema } from '../contexts';
import { validateSchemaName } from '../utils/validateSchemaName';

export const SchemaSelector = () => {
  const { projectId, selectedSchemaId, setSelectedSchemaId } =
    useSelectedSchema();
  const { data: schemas, isLoading } = useSchemas(projectId);
  const createSchemaMutation = useCreateSchema(projectId);
  const changeSchemaNameMutation = useChangeSchemaName(projectId);
  const deleteSchemaMutation = useDeleteSchema(projectId);

  const [isAdding, setIsAdding] = useState(false);
  const [newSchemaName, setNewSchemaName] = useState('');
  const [isExpanded, setIsExpanded] = useState(false);
  const {
    editingSchemaId,
    editingSchemaName,
    startEdit,
    updateEditingName,
    cancelEdit,
  } = useSchemaEditor();

  if (isLoading || !schemas) {
    return null;
  }

  const selectedSchemaName =
    schemas.find((schema) => schema.id === selectedSchemaId)?.name || '';

  const handleSchemaChange = (value: string) => {
    setSelectedSchemaId(value);
  };

  const handleAddSchema = () => {
    const trimmedName = newSchemaName.trim();
    if (!validateSchemaName(trimmedName)) {
      return;
    }

    const firstSchema = schemas[0];
    createSchemaMutation.mutate(
      {
        projectId,
        dbVendorName: firstSchema?.dbVendorName || 'mysql',
        name: trimmedName,
        charset: firstSchema?.charset || 'utf8mb4',
        collation: firstSchema?.collation || 'utf8mb4_general_ci',
      },
      {
        onSuccess: (response) => {
          if (response.data) {
            setSelectedSchemaId(response.data.id);
          }
          setNewSchemaName('');
          setIsAdding(false);
        },
      },
    );
  };

  const handleSaveEdit = (schemaId: string) => {
    const trimmedName = editingSchemaName.trim();
    if (validateSchemaName(trimmedName)) {
      changeSchemaNameMutation.mutate({
        schemaId,
        data: { newName: trimmedName },
      });
      cancelEdit();
    }
  };

  const handleCancelAdding = () => {
    setIsAdding(false);
    setNewSchemaName('');
  };

  const handleDelete = (schemaId: string) => {
    if (schemas.length <= 1) {
      toast.error('Cannot delete the last schema');
      return;
    }

    if (selectedSchemaId === schemaId) {
      const otherSchema = schemas.find((s) => s.id !== schemaId);
      if (otherSchema) {
        setSelectedSchemaId(otherSchema.id);
      }
    }
    deleteSchemaMutation.mutate(schemaId);
  };

  return (
    <div className="flex flex-col items-center bg-schemafy-bg rounded-[10px] shadow-lg p-4 transition-all duration-300 ease-in-out">
      <div className="w-full flex justify-between">
        <label className="text-sm font-heading-base text-schemafy-text">
          {isExpanded ? 'Schemas' : selectedSchemaName}
        </label>
        {isExpanded ? (
          <ChevronUp
            size={16}
            color="var(--color-schemafy-dark-gray)"
            onClick={() => setIsExpanded(false)}
            cursor="pointer"
          />
        ) : (
          <ChevronDown
            size={16}
            color="var(--color-schemafy-dark-gray)"
            onClick={() => setIsExpanded(true)}
            cursor="pointer"
          />
        )}
      </div>
      <div
        className={`flex flex-col gap-2 transition-all duration-300 ${
          isExpanded
            ? 'mt-4 h-auto opacity-100'
            : 'h-0 opacity-0 overflow-hidden'
        }`}
      >
        {schemas.map((schema) => (
          <SchemaListItem
            key={schema.id}
            schema={schema}
            isEditing={editingSchemaId === schema.id}
            editingName={editingSchemaName}
            onSelect={(schemaId) => {
              handleSchemaChange(schemaId);
              setIsExpanded(false);
            }}
            onStartEdit={startEdit}
            onSaveEdit={handleSaveEdit}
            onCancelEdit={cancelEdit}
            onDelete={handleDelete}
            onEditingNameChange={updateEditingName}
          />
        ))}
        {isAdding ? (
          <SchemaInput
            value={newSchemaName}
            onChange={setNewSchemaName}
            onSave={() => {
              handleAddSchema();
              setIsExpanded(false);
            }}
            onCancel={handleCancelAdding}
            saveLabel="Add"
          />
        ) : (
          <Button onClick={() => setIsAdding(true)} fullWidth size="dropdown">
            New Schema
          </Button>
        )}
      </div>
    </div>
  );
};
