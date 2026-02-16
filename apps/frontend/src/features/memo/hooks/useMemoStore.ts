import {
  useCallback,
  useEffect,
  useState,
  createContext,
  useContext,
} from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import * as memoApi from '../api/api';
import type { Memo, MemoComment } from '../api/types';
import {
  stringifyPosition,
  transformApiMemoToNode,
  type MemoData,
} from './memo.helper';

const SCHEMA_ID = '06DJEMTB2H3BE7JS6S0789B2FW';

export const useMemoStore = () => {
  const [storedMemos, setStoredMemos] = useState<Memo[]>([]);
  const [memos, setMemos] = useState<Node<MemoData>[]>([]);

  useEffect(() => {
    setMemos(storedMemos.map(transformApiMemoToNode));
  }, [storedMemos]);

  useEffect(() => {
    if (!SCHEMA_ID) return;

    memoApi
      .getSchemaMemosWithComments(SCHEMA_ID)
      .then((res) => {
        if (!res.success) return;
        setStoredMemos(res.result as Memo[]);
      })
      .catch((error) => {
        console.error('Failed to fetch memos:', error);
      });
  }, []);

  const createMemo = async (
    position: { x: number; y: number },
    content: string,
  ) => {
    const res = await memoApi.createMemo({
      schemaId: SCHEMA_ID,
      positions: stringifyPosition(position),
      body: content,
    });
    if (res.success) {
      setStoredMemos((prev) => [res.result as Memo, ...prev]);
    }
  };

  const updateMemo = useCallback(async (id: string, positions: string) => {
    const res = await memoApi.updateMemo(id, { positions });
    if (res.success) {
      const updated = res.result as Memo;
      setStoredMemos((prev) =>
        prev.map((m) =>
          m.id === id ? { ...updated, comments: m.comments } : m,
        ),
      );
    }
  }, []);

  const deleteMemo = useCallback(async (id: string) => {
    const res = await memoApi.deleteMemo(id);
    if (res.success) {
      setStoredMemos((prev) => prev.filter((m) => m.id !== id));
    }
  }, []);

  const onMemosChange = useCallback(
    (changes: NodeChange[]) => {
      if (!SCHEMA_ID) return;

      setMemos((nds) => applyNodeChanges(changes, nds) as Node<MemoData>[]);

      changes.forEach((change) => {
        if (change.type === 'position' && !change.dragging && change.position) {
          updateMemo(change.id, stringifyPosition(change.position));
        }

        if (change.type === 'remove') {
          deleteMemo(change.id);
        }
      });
    },
    [updateMemo, deleteMemo],
  );

  const createComment = async (memoId: string, body: string) => {
    const res = await memoApi.createMemoComment(memoId, { body });
    if (res.success) {
      const comment = res.result as MemoComment;
      setStoredMemos((prev) =>
        prev.map((m) =>
          m.id === memoId ? { ...m, comments: [...m.comments, comment] } : m,
        ),
      );
    }
  };

  const updateComment = async (
    memoId: string,
    commentId: string,
    body: string,
  ) => {
    const res = await memoApi.updateMemoComment(memoId, commentId, { body });
    if (res.success) {
      const updated = res.result as MemoComment;
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
    }
  };

  const deleteComment = async (memoId: string, commentId: string) => {
    const res = await memoApi.deleteMemoComment(memoId, commentId);
    if (res.success) {
      setStoredMemos((prev) => {
        const updated = prev.map((m) =>
          m.id === memoId
            ? { ...m, comments: m.comments.filter((c) => c.id !== commentId) }
            : m,
        );
        return updated.filter((m) => m.id !== memoId || m.comments.length > 0);
      });
    }
  };

  return {
    memos,
    onMemosChange,
    createMemo,
    updateMemo,
    deleteMemo,
    createComment,
    updateComment,
    deleteComment,
  };
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
