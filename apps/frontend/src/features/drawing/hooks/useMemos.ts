import { useState, useCallback, useEffect, useMemo } from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import { useMemoStore } from '@/store';
import {
  type MemoData,
  transformApiMemoToNode,
  stringifyPosition,
} from './memo.helper';

export type { MemoData };

export const useMemos = () => {
  const schemaId = '06DJEMTB2H3BE7JS6S0789B2FW';

  const memosBySchema = useMemoStore((state) => state.memosBySchema);
  const fetchSchemaMemos = useMemoStore((state) => state.fetchSchemaMemos);
  const createMemo = useMemoStore((state) => state.createMemo);
  const updateMemo = useMemoStore((state) => state.updateMemo);
  const deleteMemoFromStore = useMemoStore((state) => state.deleteMemo);
  const createMemoComment = useMemoStore((state) => state.createMemoComment);
  const updateMemoComment = useMemoStore((state) => state.updateMemoComment);
  const deleteMemoComment = useMemoStore((state) => state.deleteMemoComment);

  const [memos, setMemos] = useState<Node<MemoData>[]>([]);

  const memosFromStore = useMemo(
    () => (schemaId ? memosBySchema[schemaId] ?? [] : []),
    [schemaId, memosBySchema],
  );

  useEffect(() => {
    if (!schemaId) {
      setMemos([]);
      return;
    }
    fetchSchemaMemos(schemaId).catch((error) => {
      console.error('Failed to fetch memos:', error);
    });
  }, [schemaId, fetchSchemaMemos]);

  useEffect(() => {
    setMemos(memosFromStore.map(transformApiMemoToNode));
  }, [memosFromStore]);

  const addMemo = useCallback(
    (position: { x: number; y: number }, content = '') => {
      if (!schemaId) return null;
      createMemo({
        schemaId,
        positions: stringifyPosition(position),
        body: content,
      });
      return null;
    },
    [schemaId, createMemo],
  );

  const deleteMemo = useCallback(
    (memoId: string) => {
      if (!schemaId) return;
      deleteMemoFromStore(memoId, schemaId);
    },
    [schemaId, deleteMemoFromStore],
  );

  const onMemosChange = useCallback(
    (changes: NodeChange[]) => {
      setMemos((nds) => {
        const updatedMemos = applyNodeChanges(changes, nds) as Node<MemoData>[];

        changes.forEach((change) => {
          if (!schemaId) return;

          if (
            change.type === 'position' &&
            !change.dragging &&
            change.position
          ) {
            updateMemo(
              change.id,
              { positions: stringifyPosition(change.position) },
              schemaId,
            );
          }

          if (change.type === 'remove') {
            deleteMemoFromStore(change.id, schemaId);
          }
        });

        return updatedMemos;
      });
    },
    [schemaId, updateMemo, deleteMemoFromStore],
  );

  const createComment = useCallback(
    async (memoId: string, content: string) => {
      setMemos((prev) =>
        prev.map((n) =>
          n.id === memoId ? { ...n, data: { ...n.data, content } } : n,
        ),
      );

      const result = await createMemoComment(memoId, { body: content });

      if (!result) {
        const originalMemo = memosFromStore.find((m) => m.id === memoId);
        const originalContent =
          originalMemo?.comments && originalMemo.comments.length > 0
            ? originalMemo.comments[originalMemo.comments.length - 1].body
            : '';

        setMemos((prev) =>
          prev.map((n) =>
            n.id === memoId
              ? { ...n, data: { ...n.data, content: originalContent } }
              : n,
          ),
        );
      }
    },
    [createMemoComment, memosFromStore],
  );

  const updateComment = useCallback(
    async (memoId: string, content: string) => {
      const memo = memosFromStore.find((m) => m.id === memoId);
      const lastComment =
        memo?.comments && memo.comments.length > 0
          ? memo.comments[memo.comments.length - 1]
          : null;

      if (!lastComment) {
        console.error('No comment to update');
        return;
      }

      setMemos((prev) =>
        prev.map((n) =>
          n.id === memoId ? { ...n, data: { ...n.data, content } } : n,
        ),
      );

      const result = await updateMemoComment(memoId, lastComment.id, {
        body: content,
      });

      if (!result) {
        setMemos((prev) =>
          prev.map((n) =>
            n.id === memoId
              ? {
                  ...n,
                  data: { ...n.data, content: lastComment.body },
                }
              : n,
          ),
        );
      }
    },
    [updateMemoComment, memosFromStore],
  );

  const deleteComment = useCallback(
    async (memoId: string, commentId: string) => {
      setMemos((prev) => {
        const nextMemos: Node<MemoData>[] = [];
        for (const n of prev) {
          if (n.id !== memoId) {
            nextMemos.push(n);
            continue;
          }
          const currentComments = n.data.comments ?? [];
          const nextComments = currentComments.filter(
            (c) => c.id !== commentId,
          );

          if (nextComments.length > 0) {
            nextMemos.push({
              ...n,
              data: {
                ...n.data,
                comments: nextComments,
              },
            });
          }
        }
        return nextMemos;
      });

      const result = await deleteMemoComment(memoId, commentId);

      if (!result) {
        const originalMemo = memosFromStore.find((m) => m.id === memoId);
        if (originalMemo) {
          setMemos((prev) => {
            const exists = prev.some((n) => n.id === memoId);
            if (exists) {
              return prev.map((n) =>
                n.id === memoId
                  ? {
                      ...n,
                      data: { ...n.data, comments: originalMemo.comments },
                    }
                  : n,
              );
            } else {
              const recoveredNode = transformApiMemoToNode(originalMemo);
              return [...prev, recoveredNode];
            }
          });
        }
      }
    },
    [deleteMemoComment, memosFromStore],
  );

  return {
    memos,
    addMemo,
    onMemosChange,
    createComment,
    updateComment,
    deleteMemo,
    deleteComment,
  };
};
