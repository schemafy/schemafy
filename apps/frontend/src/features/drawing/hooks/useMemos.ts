import { useState, useCallback, useEffect } from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import { autorun } from 'mobx';
import { MemoStore } from '@/store/memo.store';
import {
  type MemoData,
  transformApiMemoToNode,
  stringifyPosition,
} from './memo.helper';

export const useMemos = () => {
  const schemaId = '06DJEMTB2H3BE7JS6S0789B2FW';
  const memoStore = MemoStore.getInstance();

  const [memos, setMemos] = useState<Node<MemoData>[]>([]);

  // MobX Store -> Local State Sync
  useEffect(() => {
    if (!schemaId) {
      setMemos([]);
      return;
    }

    // Reactive sync using autorun
    const dispose = autorun(() => {
      const storeMemos = memoStore.memosBySchema[schemaId] ?? [];
      setMemos(storeMemos.map(transformApiMemoToNode));
    });

    // Initial fetch
    memoStore.fetchSchemaMemos(schemaId).catch((error) => {
      console.error('Failed to fetch memos:', error);
    });

    return () => dispose();
  }, [schemaId, memoStore]);

  const addMemo = useCallback(
    (position: { x: number; y: number }, content = '') => {
      if (!schemaId) return null;
      memoStore.createMemo({
        schemaId,
        positions: stringifyPosition(position),
        body: content,
      });
      return null;
    },
    [schemaId, memoStore],
  );

  const onMemosChange = useCallback(
    (changes: NodeChange[]) => {
      if (!schemaId) return;
      
      setMemos((nds) => {
        const updatedMemos = applyNodeChanges(changes, nds) as Node<MemoData>[];
        
        changes.forEach((change) => {
          if (
            change.type === 'position' &&
            !change.dragging &&
            change.position
          ) {
            memoStore.updateMemo(
              change.id,
              { positions: stringifyPosition(change.position) },
              schemaId,
            );
          }

          if (change.type === 'remove') {
            memoStore.deleteMemo(change.id, schemaId);
          }
        });

        return updatedMemos;
      });
    },
    [schemaId, memoStore],
  );

  // Deprecated handlers (comment actions) are moved to MemoNode internal logic with direct store access,
  // but we can keep them here if needed for transitions or remove them.
  // User asked to clean up the store usage, so let's stick to core logic.
  // The previous refactoring removed createComment etc from CanvasPage usage.
  // We can remove them from here too to keep it clean.

  return {
    memos,
    addMemo,
    onMemosChange,
    // Provide additional helpers if needed, but Memo component uses store directly now.
  };
};
