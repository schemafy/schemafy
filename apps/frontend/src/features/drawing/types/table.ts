import type { ColumnType } from './column';

export type TableData = {
  tableName: string;
  columns: ColumnType[];
  schemaId: string;
  updateTable?: (tableId: string, newData: Partial<TableData>) => void;
};

export const HANDLE_STYLE = {
  background: '#141414',
  width: 10,
  height: 10,
  opacity: 0,
  transition: 'opacity 0.2s ease-in-out',
};

export interface TableProps {
  data: TableData;
  id: string;
}
