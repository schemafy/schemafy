import { useMemo, useCallback } from 'react';
import type { NodeChange } from '@xyflow/react';
import type { Node } from '@xyflow/react';
import type { TableData } from '../types';
import type { MemoData } from '@/features/memo/hooks/memo.helper';

interface UseCanvasNodesParams {
  tables: Node<TableData>[];
  memos: Node<MemoData>[];
  onTablesChange: (changes: NodeChange[]) => void;
  onMemosChange: (changes: NodeChange[]) => void;
  onTableDragStop?: (event: React.MouseEvent, node: Node<TableData>) => void;
  onMemoDragStop?: (event: React.MouseEvent, node: Node<MemoData>) => void;
  onTablesDelete?: (nodes: Node<TableData>[]) => void;
  onMemosDelete?: (nodes: Node<MemoData>[]) => void;
}

export const useCanvasNodes = ({
  tables,
  memos,
  onTablesChange,
  onMemosChange,
  onTableDragStop,
  onMemoDragStop,
  onTablesDelete,
  onMemosDelete,
}: UseCanvasNodesParams) => {
  const nodes = useMemo(() => [...tables, ...memos], [tables, memos]);

  const tableIds = useMemo(() => new Set(tables.map((t) => t.id)), [tables]);
  const memoIds = useMemo(() => new Set(memos.map((m) => m.id)), [memos]);

  const handleNodesChange = useCallback(
    (changes: NodeChange[]) => {
      const tableChanges: NodeChange[] = [];
      const memoChanges: NodeChange[] = [];

      changes.forEach((change) => {
        if (!('id' in change)) return;

        if (tableIds.has(change.id)) {
          tableChanges.push(change);
        } else if (memoIds.has(change.id)) {
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
    [tableIds, memoIds, onTablesChange, onMemosChange],
  );

  const handleNodeDragStop = (event: React.MouseEvent, node: Node) => {
    if (tableIds.has(node.id) && onTableDragStop) {
      onTableDragStop(event, node as Node<TableData>);
    } else if (memoIds.has(node.id) && onMemoDragStop) {
      onMemoDragStop(event, node as Node<MemoData>);
    }
  };

  const handleNodesDelete = (deletedNodes: Node[]) => {
    const tableNodes: Node<TableData>[] = [];
    const memoNodes: Node<MemoData>[] = [];

    deletedNodes.forEach((node) => {
      if (tableIds.has(node.id)) {
        tableNodes.push(node as Node<TableData>);
      } else if (memoIds.has(node.id)) {
        memoNodes.push(node as Node<MemoData>);
      }
    });

    if (tableNodes.length > 0 && onTablesDelete) {
      onTablesDelete(tableNodes);
    }

    if (memoNodes.length > 0 && onMemosDelete) {
      onMemosDelete(memoNodes);
    }
  };

  return { nodes, handleNodesChange, handleNodeDragStop, handleNodesDelete };
};
