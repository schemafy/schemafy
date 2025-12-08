import { useState, useCallback, useEffect } from 'react';
import { type Node, type NodeChange, applyNodeChanges } from '@xyflow/react';
import { ulid } from 'ulid';
import { type Memo } from '../types/memo';
import type { Point } from '../types';

export interface MemoData extends Record<string, unknown> {
  content: string;
}

const transformMemoToNode = (memo: Memo): Node<MemoData> => {
  return {
    id: memo.id,
    type: 'memo',
    position: memo.extra?.position || { x: 0, y: 0 },
    data: {
      content: memo.content,
    },
  };
};

export const useMemos = () => {
  // TODO: ErdStore 연결
  const [memosData, setMemosData] = useState<Memo[]>([]);
  const [memos, setMemos] = useState<Node<MemoData>[]>([]);

  useEffect(() => {
    setMemos(memosData.map(transformMemoToNode));
  }, [memosData]);

  const addMemo = useCallback((position: Point, content = '') => {
    const now = new Date();
    const memoId = ulid();
    const newMemo: Memo = {
      id: memoId,
      schemaId: 'schema-id', // TODO: 나중에 실제 schemaId 사용
      elementType: 'SCHEMA',
      elementId: 'schema-id',
      userId: 'temp-user-id', // TODO: 나중에 실제 userId 사용
      content,
      parentMemoId: null,
      resolvedAt: null,
      createdAt: now,
      updatedAt: now,
      extra: {
        position,
      },
    };

    setMemosData((prev) => [...prev, newMemo]);
    return memoId;
  }, []);

  const onMemosChange = useCallback((changes: NodeChange[]) => {
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

          setMemosData((prev) =>
            prev.map((m) =>
              m.id === change.id
                ? {
                    ...m,
                    extra: {
                      ...m.extra,
                      position: change.position!,
                    },
                    updatedAt: new Date(),
                  }
                : m,
            ),
          );
        });

      changes
        .filter((change) => change.type === 'remove')
        .forEach((change) => {
          setMemosData((prev) => prev.filter((m) => m.id !== change.id));
        });

      return updatedMemos;
    });
  }, []);

  const updateMemoContent = useCallback((memoId: string, content: string) => {
    setMemosData((prev) =>
      prev.map((memo) =>
        memo.id === memoId
          ? {
              ...memo,
              content,
              updatedAt: new Date(),
            }
          : memo,
      ),
    );
  }, []);

  return {
    memos,
    addMemo,
    onMemosChange,
    updateMemoContent,
  };
};
