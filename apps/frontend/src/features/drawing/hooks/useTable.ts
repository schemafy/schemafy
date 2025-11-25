import { useState } from 'react';
import { toast } from 'sonner';
import type { ErdStore } from '@/store/erd.store';

interface UseTableProps {
  erdStore: ErdStore;
  schemaId: string;
  tableId: string;
  tableName: string;
}

export const useTable = ({
  erdStore,
  schemaId,
  tableId,
  tableName,
}: UseTableProps) => {
  const [isEditingTableName, setIsEditingTableName] = useState(false);
  const [editingTableName, setEditingTableName] = useState(tableName);

  const saveTableName = () => {
    try {
      erdStore.changeTableName(schemaId, tableId, editingTableName);
      setIsEditingTableName(false);
    } catch {
      toast.error('Failed to save table name');
    }
  };

  const deleteTable = () => {
    erdStore.deleteTable(schemaId, tableId);
  };

  return {
    isEditingTableName,
    setIsEditingTableName,
    editingTableName,
    setEditingTableName,
    saveTableName,
    deleteTable,
  };
};
