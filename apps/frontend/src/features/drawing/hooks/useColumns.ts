import type { ColumnType } from '../types';
import { generateUniqueName } from '../utils/nameGenerator';
import * as columnService from '../services/column.service';
import { toast } from 'sonner';

interface UseColumnsProps {
  schemaId: string;
  tableId: string;
  columns: ColumnType[];
}

export const useColumns = ({ schemaId, tableId, columns }: UseColumnsProps) => {
  const addColumn = async () => {
    const existingColumnNames = columns.map((col) => col.name);
    const columnName = generateUniqueName(existingColumnNames, 'Column');

    try {
      await columnService.createColumn(
        schemaId,
        tableId,
        columnName,
        columns.length,
        'VARCHAR',
        '255',
        false,
        'utf8mb4',
        'utf8mb4_general_ci',
        '',
      );
    } catch (error) {
      toast.error('Failed to create column');
      console.error(error);
    }
  };

  const removeColumn = async (columnId: string) => {
    try {
      await columnService.deleteColumn(schemaId, tableId, columnId);
    } catch (error) {
      toast.error('Failed to delete column');
      console.error(error);
    }
  };

  return {
    addColumn,
    removeColumn,
  };
};
