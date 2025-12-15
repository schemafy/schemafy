import { observer } from 'mobx-react-lite';
import { useState } from 'react';
import { toast } from 'sonner';
import { ErdStore } from '@/store/erd.store';
import * as schemaService from '../services/schema.service';
import { Button } from '@/components';
import { ChevronDown, ChevronUp } from 'lucide-react';
import { SchemaInput } from './SchemaInput';
import { SchemaListItem } from './SchemaListItem';
import { useSchemaEditor } from '../hooks';
import { validateSchemaName } from '../utils/validateSchemaName';

export const SchemaSelector = observer(() => {
  const erdStore = ErdStore.getInstance();
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

  if (erdStore.erdState.state !== 'loaded') {
    return null;
  }

  const { database } = erdStore.erdState;
  const selectedSchemaId = erdStore.selectedSchemaId;
  const selectedSchemaName =
    database.schemas.find((schema) => schema.id === selectedSchemaId)?.name ||
    '';

  const handleSchemaChange = (value: string) => {
    erdStore.selectSchema(value);
  };

  const handleAddSchema = async () => {
    const trimmedName = newSchemaName.trim();

    if (!validateSchemaName(trimmedName)) {
      return;
    }

    try {
      const newSchemaId = await schemaService.createSchema(trimmedName);
      erdStore.selectSchema(newSchemaId);

      setNewSchemaName('');
      setIsAdding(false);
    } catch (error) {
      toast.error('Failed to create schema');
      console.error(error);
    }
  };

  const handleSaveEdit = async (schemaId: string) => {
    const trimmedName = editingSchemaName.trim();

    if (!validateSchemaName(trimmedName)) {
      return;
    }

    try {
      await schemaService.updateSchemaName(schemaId, trimmedName);
      cancelEdit();
    } catch (error) {
      toast.error('Failed to update schema name');
      console.error(error);
    }
  };

  const handleCancelAdding = () => {
    setIsAdding(false);
    setNewSchemaName('');
  };

  const handleDelete = async (schemaId: string) => {
    if (database.schemas.length <= 1) {
      toast.error('Cannot delete the last schema');
      return;
    }

    if (selectedSchemaId === schemaId) {
      const otherSchema = database.schemas.find((s) => s.id !== schemaId);
      if (otherSchema) {
        erdStore.selectSchema(otherSchema.id);
      }
    }

    try {
      await schemaService.deleteSchema(schemaId);
    } catch (error) {
      toast.error('Failed to delete schema');
      console.error(error);
    }
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
        {database.schemas.map((schema) => (
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
});
