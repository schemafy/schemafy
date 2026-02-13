import { useState } from 'react';
import { toast } from 'sonner';
import { useChangeTableName, useDeleteTable } from './useTableMutations';

interface UseTableProps {
  schemaId: string;
  tableId: string;
  tableName: string;
}

export const useTable = ({ schemaId, tableId, tableName }: UseTableProps) => {
  const [isEditingTableName, setIsEditingTableName] = useState(false);
  const [editingTableName, setEditingTableName] = useState(tableName);

  const changeTableNameMutation = useChangeTableName(schemaId);
  const deleteTableMutation = useDeleteTable(schemaId);

  const saveTableName = () => {
    changeTableNameMutation.mutate(
      {
        tableId,
        data: { newName: editingTableName },
      },
      {
        onSuccess: () => {
          setIsEditingTableName(false);
        },
        onError: () => {
          toast.error('Failed to save table name');
        },
      },
    );
  };

  const deleteTable = () => {
    deleteTableMutation.mutate(tableId);
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
