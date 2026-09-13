import type { ColumnType } from './column';
import type { IndexDataType } from './indexTypes';
import type { Constraint } from '@/types';

export type TableData = {
  tableName: string;
  columns: ColumnType[];
  indexes: IndexDataType[];
  constraints: Constraint[];
  schemaId: string;
  updateTable?: (tableId: string, newData: Partial<TableData>) => void;
};

export const HANDLE_STYLE = {
  background: 'hsl(var(--schemafy-soft-blue) / 0.78)',
  border: '1px solid var(--color-schemafy-bg)',
  boxShadow: '0 0 0 1px hsl(var(--schemafy-soft-blue) / 0.16)',
  width: 8,
  height: 8,
  opacity: 0,
  transition: 'opacity 0.18s ease-in-out',
};

export interface TableProps {
  data: TableData;
  id: string;
}
