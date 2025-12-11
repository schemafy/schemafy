import { useState, useCallback, useEffect, useMemo } from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import { useMemoStore } from '@/store';
import type { Memo as ApiMemo } from '@/lib/api/memo';
import { ErdStore } from '@/store/erd.store';

export interface MemoData extends Record<string, unknown> {
  content: string;
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
    // ignore
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
    },
  };
};

export const useMemos = () => {
  const erdStore = ErdStore.getInstance();
  const schemaId = erdStore.selectedSchemaId;

  const {
    memosBySchema,
    fetchSchemaMemos,
    createMemo,
    updateMemo,
    deleteMemo,
    createMemoComment,
  } = useMemoStore();

  const [memos, setMemos] = useState<Node<MemoData>[]>([]);

  useEffect(() => {
    if (!schemaId) {
      setMemos([]);
      return;
    }
    fetchSchemaMemos(schemaId).catch(() => {
      // 에러는 store.error로 노출
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
      void createMemo({
        schemaId,
        positions: stringifyPosition(position),
        body: content,
      });
      return null;
    },
    [schemaId, createMemo],
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

            // 서버 위치 업데이트
            if (schemaId) {
              void updateMemo(
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
              void deleteMemo(change.id, schemaId);
            }
          });

        return updatedMemos;
      });
    },
    [schemaId, updateMemo, deleteMemo],
  );

  const updateMemoContent = useCallback(
    (memoId: string, content: string) => {
      void createMemoComment(memoId, { body: content });
      setMemos((prev) =>
        prev.map((n) =>
          n.id === memoId ? { ...n, data: { ...n.data, content } } : n,
        ),
      );
    },
    [createMemoComment],
  );

  return {
    memos,
    addMemo,
    onMemosChange,
    updateMemoContent,
  };
};
