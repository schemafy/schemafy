import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react';
import { applyNodeChanges, type Node, type NodeChange } from '@xyflow/react';
import * as memoApi from '../api/api';
import type { Memo } from '../api/types';
import {
  type MemoData,
  stringifyPosition,
  transformApiMemoToNode,
} from './memo.helper';
import { useErdHistory } from '@/features/drawing/history';
import {
  CreateMemoCommand,
  DeleteMemoCommand,
  MoveMemoCommand,
  CreateCommentCommand,
  DeleteCommentCommand,
  UpdateCommentCommand,
} from '../history/MemoCommands';

export const useMemoStore = (schemaId: string) => {
  const { push } = useErdHistory();
  const [storedMemos, setStoredMemos] = useState<Memo[]>([]);
  const [memos, setMemos] = useState<Node<MemoData>[]>([]);

  const storedMemosRef = useRef(storedMemos);
  storedMemosRef.current = storedMemos;

  const previousPositionsRef = useRef<Record<string, string>>({});

  useEffect(() => {
    setMemos(storedMemos.map(transformApiMemoToNode));
  }, [storedMemos]);

  useEffect(() => {
    if (!schemaId) return;
    memoApi
      .getSchemaMemosWithComments(schemaId)
      .then(setStoredMemos)
      .catch(console.error);
  }, [schemaId]);

  const createMemo = async (
    position: { x: number; y: number },
    content: string,
  ) => {
    try {
      const originalRequest = {
        schemaId,
        positions: stringifyPosition(position),
        body: content,
      };
      const memo = await memoApi.createMemo(originalRequest);
      setStoredMemos((prev) => [memo, ...prev]);
      push(
        new CreateMemoCommand({
          memoId: memo.id,
          originalRequest,
          setStoredMemos,
        }),
      );
    } catch {}
  };

  const updateMemo = useCallback(
    async (id: string, positions: string, previousPositions?: string) => {
      try {
        const updated = await memoApi.updateMemo(id, { positions });
        setStoredMemos((prev) =>
          prev.map((m) =>
            m.id === id ? { ...updated, comments: m.comments } : m,
          ),
        );
        if (previousPositions !== undefined && previousPositions !== positions) {
          push(
            new MoveMemoCommand({
              memoId: id,
              previousPositions,
              newPositions: positions,
              setStoredMemos,
            }),
          );
        }
      } catch {}
    },
    [push],
  );

  const deleteMemo = useCallback(
    async (id: string) => {
      try {
        delete previousPositionsRef.current[id];
        const memo = storedMemosRef.current.find((m) => m.id === id);
        await memoApi.deleteMemo(id);
        setStoredMemos((prev) => prev.filter((m) => m.id !== id));
        if (memo) {
          push(new DeleteMemoCommand({ memo, setStoredMemos }));
        }
      } catch {}
    },
    [push],
  );

  const onMemosChange = useCallback(
    (changes: NodeChange[]) => {
      setMemos((nds) => applyNodeChanges(changes, nds) as Node<MemoData>[]);

      changes.forEach((change) => {
        if (change.type === 'position') {
          if (change.dragging) {
            if (!previousPositionsRef.current[change.id]) {
              const memo = storedMemosRef.current.find(
                (m) => m.id === change.id,
              );
              if (memo) {
                previousPositionsRef.current[change.id] = memo.positions;
              }
            }
          } else if (change.position) {
            const previousPositions = previousPositionsRef.current[change.id];
            delete previousPositionsRef.current[change.id];
            void updateMemo(
              change.id,
              stringifyPosition(change.position),
              previousPositions,
            );
          }
        }

        if (change.type === 'remove') {
          void deleteMemo(change.id);
        }
      });
    },
    [updateMemo, deleteMemo],
  );

  const createComment = async (memoId: string, body: string) => {
    try {
      const comment = await memoApi.createMemoComment(memoId, { body });
      setStoredMemos((prev) =>
        prev.map((m) =>
          m.id === memoId ? { ...m, comments: [...m.comments, comment] } : m,
        ),
      );
      push(
        new CreateCommentCommand({
          memoId,
          commentId: comment.id,
          originalBody: body,
          setStoredMemos,
        }),
      );
    } catch {}
  };

  const updateComment = async (
    memoId: string,
    commentId: string,
    body: string,
  ) => {
    try {
      const previousBody =
        storedMemosRef.current
          .find((m) => m.id === memoId)
          ?.comments.find((c) => c.id === commentId)?.body ?? '';
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
      push(
        new UpdateCommentCommand({
          memoId,
          commentId,
          previousBody,
          newBody: body,
          setStoredMemos,
        }),
      );
    } catch {}
  };

  const deleteComment = async (memoId: string, commentId: string) => {
    try {
      const comment = storedMemosRef.current
        .find((m) => m.id === memoId)
        ?.comments.find((c) => c.id === commentId);
      await memoApi.deleteMemoComment(memoId, commentId);
      setStoredMemos((prev) =>
        prev.map((m) =>
          m.id === memoId
            ? { ...m, comments: m.comments.filter((c) => c.id !== commentId) }
            : m,
        ),
      );
      if (comment) {
        push(new DeleteCommentCommand({ memoId, comment, setStoredMemos }));
      }
    } catch {}
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
