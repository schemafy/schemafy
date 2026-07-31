import { memo, useState } from 'react';
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

export const SchemaSelector = memo(() => {
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
      toast.error('Schema name must be 1-20 characters');
      return;
    }

    const firstSchema = schemas[0];
    createSchemaMutation.mutate(
      {
        projectId,
        name: trimmedName,
        charset: firstSchema?.charset,
        collation: firstSchema?.collation,
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
    if (!validateSchemaName(trimmedName)) {
      toast.error('Schema name must be 1-20 characters');
      return;
    }
    changeSchemaNameMutation.mutate({
      schemaId,
      data: { newName: trimmedName },
    });
    cancelEdit();
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
    <div className="schemafy-canvas-panel flex min-w-[14rem] flex-col rounded-2xl p-3 transition-all duration-300 ease-in-out">
      <button
        type="button"
        className="schemafy-focus-ring flex h-10 w-full items-center justify-between gap-3 rounded-xl px-3 text-left transition-colors hover:bg-schemafy-secondary"
        onClick={() => setIsExpanded((prev) => !prev)}
        aria-label={isExpanded ? 'Collapse schemas' : 'Expand schemas'}
      >
        <span className="min-w-0 truncate text-sm font-heading-base text-schemafy-text">
          {isExpanded ? 'Schemas' : selectedSchemaName}
        </span>
        {isExpanded ? (
          <ChevronUp size={16} className="shrink-0 text-schemafy-dark-gray" />
        ) : (
          <ChevronDown size={16} className="shrink-0 text-schemafy-dark-gray" />
        )}
      </button>
      <div
        className={`flex w-full flex-col gap-2.5 transition-all duration-300 ${
          isExpanded
            ? 'mt-3 h-auto opacity-100'
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
          <Button
            onClick={() => setIsAdding(true)}
            fullWidth
            size="dropdown"
            className="h-10"
          >
            New Schema
          </Button>
        )}
      </div>
    </div>
  );
});
