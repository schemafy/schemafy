import { useState } from 'react';

export const useSchemaEditor = () => {
  const [editingSchemaId, setEditingSchemaId] = useState<string | null>(null);
  const [editingSchemaName, setEditingSchemaName] = useState('');

  const startEdit = (schemaId: string, currentName: string) => {
    setEditingSchemaId(schemaId);
    setEditingSchemaName(currentName);
  };

  const updateEditingName = (name: string) => {
    setEditingSchemaName(name);
  };

  const cancelEdit = () => {
    setEditingSchemaId(null);
    setEditingSchemaName('');
  };

  const resetEdit = () => {
    setEditingSchemaId(null);
    setEditingSchemaName('');
  };

  return {
    editingSchemaId,
    editingSchemaName,
    startEdit,
    updateEditingName,
    cancelEdit,
    resetEdit,
  };
};
