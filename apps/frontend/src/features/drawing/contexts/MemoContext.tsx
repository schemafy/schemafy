import React, {
  createContext,
  useContext,
  useCallback,
  useMemo,
  useState,
  useEffect,
} from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import { autorun } from 'mobx';
import { MemoStore } from '@/store';
import {
  stringifyPosition,
  transformApiMemoToNode,
  type MemoData,
} from '../hooks/memo.helper';

const SCHEMA_ID = '06DJEMTB2H3BE7JS6S0789B2FW';

interface MemoContextType {
  memos: Node<MemoData>[];
  onMemosChange: (changes: NodeChange[]) => void;
  createMemo: (
    position: { x: number; y: number },
    content: string,
  ) => Promise<void>;
  updateMemo: (id: string, positions: string) => Promise<void>;
  deleteMemo: (id: string) => Promise<void>;
  createComment: (memoId: string, body: string) => Promise<void>;
  updateComment: (
    memoId: string,
    commentId: string,
    body: string,
  ) => Promise<void>;
  deleteComment: (memoId: string, commentId: string) => Promise<void>;
}

const MemoContext = createContext<MemoContextType | null>(null);

export const MemoProvider = ({ children }: { children: React.ReactNode }) => {
  const memoStore = MemoStore.getInstance();
  const [memos, setMemos] = useState<Node<MemoData>[]>([]);

  useEffect(() => {
    if (!SCHEMA_ID) {
      setMemos([]);
      return;
    }

    const dispose = autorun(() => {
      const storeMemos = memoStore.memosBySchema[SCHEMA_ID] ?? [];
      setMemos(storeMemos.map(transformApiMemoToNode));
    });

    memoStore.fetchSchemaMemos(SCHEMA_ID).catch((error) => {
      console.error('Failed to fetch memos:', error);
    });

    return () => dispose();
  }, [memoStore]);

  const createMemo = useCallback(
    async (position: { x: number; y: number }, content: string) => {
      await memoStore.createMemo({
        schemaId: SCHEMA_ID,
        positions: stringifyPosition(position),
        body: content,
      });
    },
    [memoStore],
  );

  const updateMemo = useCallback(
    async (id: string, positions: string) => {
      await memoStore.updateMemo(id, { positions }, SCHEMA_ID);
    },
    [memoStore],
  );

  const deleteMemo = useCallback(
    async (id: string) => {
      await memoStore.deleteMemo(id, SCHEMA_ID);
    },
    [memoStore],
  );

  const onMemosChange = useCallback(
    (changes: NodeChange[]) => {
      if (!SCHEMA_ID) return;

      setMemos((nds) => {
        const updatedMemos = applyNodeChanges(changes, nds) as Node<MemoData>[];

        changes.forEach((change) => {
          if (
            change.type === 'position' &&
            !change.dragging &&
            change.position
          ) {
            updateMemo(change.id, stringifyPosition(change.position));
          }

          if (change.type === 'remove') {
            deleteMemo(change.id);
          }
        });

        return updatedMemos;
      });
    },
    [updateMemo, deleteMemo],
  );

  const createComment = useCallback(
    async (memoId: string, body: string) => {
      await memoStore.createMemoComment(memoId, { body });
    },
    [memoStore],
  );

  const updateComment = useCallback(
    async (memoId: string, commentId: string, body: string) => {
      await memoStore.updateMemoComment(memoId, commentId, { body });
    },
    [memoStore],
  );

  const deleteComment = useCallback(
    async (memoId: string, commentId: string) => {
      await memoStore.deleteMemoComment(memoId, commentId);
    },
    [memoStore],
  );

  const value = useMemo(
    () => ({
      memos,
      onMemosChange,
      createMemo,
      updateMemo,
      deleteMemo,
      createComment,
      updateComment,
      deleteComment,
    }),
    [
      memos,
      onMemosChange,
      createMemo,
      updateMemo,
      deleteMemo,
      createComment,
      updateComment,
      deleteComment,
    ],
  );

  return <MemoContext.Provider value={value}>{children}</MemoContext.Provider>;
};

export const useMemoContext = () => {
  const context = useContext(MemoContext);
  if (!context) {
    throw new Error('useMemoContext must be used within a MemoProvider');
  }
  return context;
};
