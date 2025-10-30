import { ulid } from 'ulid';
import type { ErdStore } from '@/store/erd.store';
import type { ColumnType } from '../../types';

interface UseColumnActionsProps {
  erdStore: ErdStore;
  schemaId: string;
  tableId: string;
  columns: ColumnType[];
}

export const useColumnActions = ({ erdStore, schemaId, tableId, columns }: UseColumnActionsProps) => {
  const addColumn = () => {
    erdStore.createColumn(schemaId, tableId, {
      id: ulid(),
      name: `newColumn${columns.length + 1}`,
      ordinalPosition: columns.length,
      dataType: 'VARCHAR',
      lengthScale: '255',
      isAutoIncrement: false,
      charset: 'utf8mb4',
      collation: 'utf8mb4_general_ci',
    });
  };

  const removeColumn = (columnId: string) => {
    erdStore.deleteColumn(schemaId, tableId, columnId);
  };

  return {
    addColumn,
    removeColumn,
  };
};
