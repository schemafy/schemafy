import { useState, useCallback, useEffect, useMemo } from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import { useMemoStore } from '@/store';
import type { Memo as ApiMemo } from '@/lib/api/memo';
//import { ErdStore } from '@/store/erd.store';

export interface MemoData extends Record<string, unknown> {
  content: string;
  comments: ApiMemo['comments'];
  createComment?: (content: string) => Promise<void>;
  updateComment?: (content: string) => Promise<void>;
  deleteMemo?: () => Promise<void>;
}

const safeParsePosition = (positions: string): { x: number; y: number } => {
  if (!positions) return { x: 0, y: 0 };
  try {
    const parsed = JSON.parse(positions);
    if (
      parsed &&
      typeof parsed === 'object' &&
      typeof parsed.x === 'number' &&
      typeof parsed.y === 'number'
    ) {
      return { x: parsed.x, y: parsed.y };
    }
  } catch {
    console.error('Failed to parse positions');
  }
  return { x: 0, y: 0 };
};

const stringifyPosition = (pos: { x: number; y: number }): string =>
  JSON.stringify({ x: pos.x, y: pos.y });

const transformApiMemoToNode = (memo: ApiMemo): Node<MemoData> => {
  const position = safeParsePosition(memo.positions);
  const lastCommentBody =
    memo.comments && memo.comments.length > 0
      ? (memo.comments[memo.comments.length - 1]?.body ?? '')
      : '';
  return {
    id: memo.id,
    type: 'memo',
    position,
    data: {
      content: lastCommentBody,
      comments: memo.comments,
    },
  };
};

export const useMemos = () => {
  //const erdStore = ErdStore.getInstance();
  const schemaId = '06DJEMTB2H3BE7JS6S0789B2FW'; //erdStore.selectedSchemaId;

  const {
    memosBySchema,
    fetchSchemaMemos,
    createMemo,
    updateMemo,
    deleteMemo: deleteMemoFromStore,
    createMemoComment,
    updateMemoComment,
  } = useMemoStore();

  const [memos, setMemos] = useState<Node<MemoData>[]>([]);

  useEffect(() => {
    if (!schemaId) {
      setMemos([]);
      return;
    }
    fetchSchemaMemos(schemaId).catch(() => {
      console.error('Failed to fetch memos');
    });
  }, [schemaId, fetchSchemaMemos]);

  const memosFromStore: ApiMemo[] = useMemo(
    () => (schemaId ? (memosBySchema[schemaId] ?? []) : []),
    [schemaId, memosBySchema],
  );

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

        changes
          .filter(
            (change) =>
              change.type === 'position' &&
              change.dragging === false &&
              change.position,
          )
          .forEach((change) => {
            if (change.type !== 'position' || !change.position) return;

            const memo = nds.find((m) => m.id === change.id);
            if (!memo) return;
            
            if (schemaId) {
              updateMemo(
                change.id,
                {
                  positions: stringifyPosition(change.position),
                },
                schemaId,
              );
            }
          });

        changes
          .filter((change) => change.type === 'remove')
          .forEach((change) => {
            if (schemaId) {
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
    [createMemoComment],
  );

  const updateComment = useCallback(
    async (memoId: string, content: string) => {
      const memo = memosFromStore.find((m) => m.id === memoId);
      const comments = memo?.comments;
      const lastComment =
        comments && comments.length > 0 ? comments[comments.length - 1] : null;

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
        const originalContent = lastComment.body;
        setMemos((prev) =>
          prev.map((n) =>
            n.id === memoId
              ? { ...n, data: { ...n.data, content: originalContent } }
              : n,
          ),
        );
      }
    },
    [updateMemoComment],
  );

  return {
    memos,
    addMemo,
    onMemosChange,
    createComment,
    updateComment,
    deleteMemo,
  };
};
