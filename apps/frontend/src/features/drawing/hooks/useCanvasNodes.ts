import { useCallback, useMemo } from 'react';
import type { NodeChange } from '@xyflow/react';
import type { Node } from '@xyflow/react';
import type { TableData } from '../types';
import type { MemoData } from '@/features/memo/hooks/memo.helper';

interface UseCanvasNodesParams {
  tables: Node<TableData>[];
  memos: Node<MemoData>[];
  onTablesChange: (changes: NodeChange[]) => void;
  onMemosChange: (changes: NodeChange[]) => void;
}

export const useCanvasNodes = ({
  tables,
  memos,
  onTablesChange,
  onMemosChange,
}: UseCanvasNodesParams) => {
  const nodes = useMemo(() => [...tables, ...memos], [tables, memos]);

  const handleNodesChange = useCallback(
    (changes: NodeChange[]) => {
      const tableChanges: NodeChange[] = [];
      const memoChanges: NodeChange[] = [];

      changes.forEach((change) => {
        if (!('id' in change)) return;

        const isTable = tables.some((t) => t.id === change.id);
        const isMemo = memos.some((m) => m.id === change.id);

        if (isTable) {
          tableChanges.push(change);
        } else if (isMemo) {
          memoChanges.push(change);
        }
      });

      if (tableChanges.length > 0) {
        onTablesChange(tableChanges);
      }

      if (memoChanges.length > 0) {
        onMemosChange(memoChanges);
      }
    },
    [tables, memos, onTablesChange, onMemosChange],
  );

  return { nodes, handleNodesChange };
};
