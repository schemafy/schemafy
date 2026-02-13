import type { ColumnType } from '../types';
import { generateUniqueName } from '../utils/nameGenerator';
import { useCreateColumn, useDeleteColumn } from './useColumnMutations';

interface UseColumnsProps {
  schemaId: string;
  tableId: string;
  columns: ColumnType[];
}

export const useColumns = ({ schemaId, tableId, columns }: UseColumnsProps) => {
  const createColumnMutation = useCreateColumn(schemaId);
  const deleteColumnMutation = useDeleteColumn(schemaId);

  const addColumn = () => {
    const existingColumnNames = columns.map((col) => col.name);

    createColumnMutation.mutate({
      tableId,
      name: generateUniqueName(existingColumnNames, 'Column'),
      dataType: 'VARCHAR',
      length: 255,
      autoIncrement: false,
      charset: 'utf8mb4',
      collation: 'utf8mb4_general_ci',
    });
  };

  const removeColumn = (columnId: string) => {
    deleteColumnMutation.mutate(columnId);
  };

  return {
    addColumn,
    removeColumn,
  };
};
