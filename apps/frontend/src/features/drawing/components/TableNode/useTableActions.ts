import { useState } from 'react';
import type { ErdStore } from '@/store/erd.store';

interface UseTableActionsProps {
  erdStore: ErdStore;
  schemaId: string;
  tableId: string;
  tableName: string;
}

export const useTableActions = ({ erdStore, schemaId, tableId, tableName }: UseTableActionsProps) => {
  const [isEditingTableName, setIsEditingTableName] = useState(false);
  const [editingTableName, setEditingTableName] = useState(tableName);

  const saveTableName = () => {
    erdStore.changeTableName(schemaId, tableId, editingTableName);
    setIsEditingTableName(false);
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
