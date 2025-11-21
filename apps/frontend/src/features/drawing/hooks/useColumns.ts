import { ulid } from 'ulid';
import type { ErdStore } from '@/store/erd.store';
import type { ColumnType } from '../types';
import { generateUniqueName } from '../utils/nameGenerator';

interface UseColumnsProps {
  erdStore: ErdStore;
  schemaId: string;
  tableId: string;
  columns: ColumnType[];
}

export const useColumns = ({
  erdStore,
  schemaId,
  tableId,
  columns,
}: UseColumnsProps) => {
  const addColumn = () => {
    const existingColumnNames = columns.map((col) => col.name);

    erdStore.createColumn(schemaId, tableId, {
      id: ulid(),
      name: generateUniqueName(existingColumnNames, 'Column'),
      ordinalPosition: columns.length,
      dataType: 'VARCHAR',
      lengthScale: '255',
      isAutoIncrement: false,
      charset: 'utf8mb4',
      collation: 'utf8mb4_general_ci',
      isAffected: false,
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
