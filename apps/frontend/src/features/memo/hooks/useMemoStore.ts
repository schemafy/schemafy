import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { applyNodeChanges, type Node, type NodeChange } from '@xyflow/react';
import * as memoApi from '../api/api';
import type { Memo } from '../api/types';
import { type MemoData, transformApiMemoToNode } from './memo.helper';
import { useSelectedSchema } from '@/features';
import { useLatest } from '@/features/drawing/hooks/useLatest';

export const useMemoStore = () => {
  const [storedMemos, setStoredMemos] = useState<Memo[]>([]);
  const [memos, setMemos] = useState<Node<MemoData>[]>([]);

  const { selectedSchemaId } = useSelectedSchema();
  const selectedSchemaIdRef = useLatest(selectedSchemaId);

  useEffect(() => {
    setMemos(storedMemos.map(transformApiMemoToNode));
  }, [storedMemos]);

  useEffect(() => {
    if (!selectedSchemaId) {
      setStoredMemos([]);
      return;
    }

    let cancelled = false;

    memoApi
      .getSchemaMemosWithComments(selectedSchemaId)
      .then((memos) => {
        if (!cancelled) {
          setStoredMemos(memos);
        }
      })
      .catch((error) => {
        console.error('Failed to fetch memos:', error);
      });

    return () => {
      cancelled = true;
    };
  }, [selectedSchemaId]);

  const createMemo = useCallback(
    async (position: { x: number; y: number }, content: string) => {
      const schemaId = selectedSchemaIdRef.current;
      if (!schemaId) return;

      try {
        const memo = await memoApi.createMemo({
          schemaId,
          positions: { x: position.x, y: position.y },
          body: content,
        });
        setStoredMemos((prev) => [memo, ...prev]);
      } catch {}
    },
    [selectedSchemaIdRef],
  );

  const updateMemo = useCallback(
    async (id: string, positions: Memo['positions']) => {
      try {
        const updated = await memoApi.updateMemo(id, { positions });
        setStoredMemos((prev) =>
          prev.map((m) =>
            m.id === id ? { ...updated, comments: m.comments } : m,
          ),
        );
      } catch {}
    },
    [],
  );

  const deleteMemo = useCallback(async (id: string) => {
    try {
      await memoApi.deleteMemo(id);
      setStoredMemos((prev) => prev.filter((m) => m.id !== id));
    } catch {}
  }, []);

  const onMemosChange = useCallback(
    (changes: NodeChange[]) => {
      if (!selectedSchemaIdRef.current) return;

      setMemos((nds) => applyNodeChanges(changes, nds) as Node<MemoData>[]);

      changes.forEach((change) => {
        if (change.type === 'position' && !change.dragging && change.position) {
          updateMemo(change.id, { x: change.position.x, y: change.position.y });
        }

        if (change.type === 'remove') {
          deleteMemo(change.id);
        }
      });
    },
    [deleteMemo, selectedSchemaIdRef, updateMemo],
  );

  const createComment = useCallback(async (memoId: string, body: string) => {
    try {
      const comment = await memoApi.createMemoComment(memoId, { body });
      setStoredMemos((prev) =>
        prev.map((m) =>
          m.id === memoId ? { ...m, comments: [...m.comments, comment] } : m,
        ),
      );
    } catch {}
  }, []);

  const updateComment = useCallback(
    async (memoId: string, commentId: string, body: string) => {
      try {
        const updated = await memoApi.updateMemoComment(memoId, commentId, {
          body,
        });
        setStoredMemos((prev) =>
          prev.map((m) =>
            m.id === memoId
              ? {
                  ...m,
                  comments: m.comments.map((c) =>
                    c.id === commentId ? updated : c,
                  ),
                }
              : m,
          ),
        );
      } catch {}
    },
    [],
  );

  const deleteComment = useCallback(
    async (memoId: string, commentId: string) => {
      try {
        await memoApi.deleteMemoComment(memoId, commentId);
        setStoredMemos((prev) => {
          const updated = prev.map((m) =>
            m.id === memoId
              ? { ...m, comments: m.comments.filter((c) => c.id !== commentId) }
              : m,
          );
          return updated.filter(
            (m) => m.id !== memoId || m.comments.length > 0,
          );
        });
      } catch {}
    },
    [],
  );

  return useMemo(
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
};

export type MemoStoreValue = ReturnType<typeof useMemoStore>;

export const MemoContext = createContext<MemoStoreValue | null>(null);

export const useMemoContext = () => {
  const context = useContext(MemoContext);
  if (!context) {
    throw new Error('useMemoContext must be used within a MemoProvider');
  }
  return context;
};
