import { useState } from 'react';
import { toast } from 'sonner';
import * as tableService from '../services/table.service';

interface UseTableProps {
  schemaId: string;
  tableId: string;
  tableName: string;
}

export const useTable = ({ schemaId, tableId, tableName }: UseTableProps) => {
  const [isEditingTableName, setIsEditingTableName] = useState(false);
  const [editingTableName, setEditingTableName] = useState(tableName);

  const saveTableName = async () => {
    try {
      await tableService.updateTableName(schemaId, tableId, editingTableName);
      setIsEditingTableName(false);
    } catch (error) {
      toast.error('Failed to save table name');
      console.error(error);
    }
  };

  const deleteTable = async () => {
    try {
      await tableService.deleteTable(schemaId, tableId);
    } catch (error) {
      toast.error('Failed to delete table');
      console.error(error);
    }
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
