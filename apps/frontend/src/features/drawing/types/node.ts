import type { FieldType } from './field';

export type TableNodeData = {
  tableName: string;
  fields: FieldType[];
  updateNode?: (nodeId: string, newData: Partial<TableNodeData>) => void;
};

export const HANDLE_STYLE = {
  background: '#141414',
  width: 10,
  height: 10,
  opacity: 0,
  transition: 'opacity 0.2s ease-in-out',
};

export interface TableNodeProps {
  data: TableNodeData;
  id: string;
}
