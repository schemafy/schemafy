import { create } from 'zustand';
import * as memoApi from '@/lib/api/memo';
import type {
  Memo,
  MemoComment,
  CreateMemoRequest,
  UpdateMemoRequest,
  CreateMemoCommentRequest,
  UpdateMemoCommentRequest,
} from '@/lib/api/memo/types';

type MemosBySchema = Record<string, Memo[]>;
type CommentsByMemo = Record<string, MemoComment[]>;

export interface MemoState {
  memosBySchema: MemosBySchema;
  commentsByMemo: CommentsByMemo;
  isLoading: boolean;
  error: string | null;

  fetchSchemaMemos: (schemaId: string) => Promise<void>;

  createMemo: (data: CreateMemoRequest) => Promise<Memo | null>;
  updateMemo: (
    memoId: string,
    data: UpdateMemoRequest,
    schemaId?: string,
  ) => Promise<Memo | null>;
  deleteMemo: (memoId: string, schemaId: string) => Promise<boolean>;

  fetchMemoComments: (memoId: string) => Promise<void>;
  createMemoComment: (
    memoId: string,
    data: CreateMemoCommentRequest,
  ) => Promise<MemoComment | null>;
  updateMemoComment: (
    memoId: string,
    commentId: string,
    data: UpdateMemoCommentRequest,
  ) => Promise<MemoComment | null>;
  deleteMemoComment: (memoId: string, commentId: string) => Promise<boolean>;

  clearError: () => void;
}

export const useMemoStore = create<MemoState>((set, get) => ({
  memosBySchema: {},
  commentsByMemo: {},
  isLoading: false,
  error: null,

  fetchSchemaMemos: async (schemaId: string) => {
    set({ isLoading: true, error: null });
    try {
      const res = await memoApi.getSchemaMemos(schemaId);
      if (!res.success || !res.result) {
        set({ error: res.error?.message ?? 'Failed to fetch memos' });
        return;
      }
      const memos = res.result;

      const commentsResults = await Promise.allSettled(
        memos.map((memo) => memoApi.getMemoComments(memo.id)),
      );

      const combinedMemos = memos.map((memo, index) => {
        const result = commentsResults[index];
        const comments =
          result.status === 'fulfilled' &&
          result.value.success &&
          result.value.result
            ? result.value.result
            : [];
        return { ...memo, comments };
      });

      const newCommentsByMemo = combinedMemos.reduce((acc, memo) => {
        acc[memo.id] = memo.comments ?? [];
        return acc;
      }, {} as CommentsByMemo);

      set((state) => ({
        memosBySchema: {
          ...state.memosBySchema,
          [schemaId]: combinedMemos,
        },
        commentsByMemo: {
          ...state.commentsByMemo,
          ...newCommentsByMemo,
        },
        isLoading: false,
      }));
    } catch (e) {
      set({
        isLoading: false,
        error: e instanceof Error ? e.message : 'Failed to fetch memos',
      });
    } finally {
      set({ isLoading: false });
    }
  },

  createMemo: async (data: CreateMemoRequest) => {
    set({ error: null });
    try {
      const res = await memoApi.createMemo(data);
      if (!res.success || !res.result) {
        set({ error: res.error?.message ?? 'Failed to create memo' });
        return null;
      }
      const memo = res.result;
      set((state) => {
        const list = state.memosBySchema[data.schemaId] ?? [];
        return {
          memosBySchema: {
            ...state.memosBySchema,
            [data.schemaId]: [memo, ...list],
          },
        };
      });
      return memo;
    } catch (e) {
      set({ error: e instanceof Error ? e.message : 'Failed to create memo' });
      return null;
    }
  },

  updateMemo: async (
    memoId: string,
    data: UpdateMemoRequest,
    schemaId?: string,
  ) => {
    set({ error: null });
    try {
      const res = await memoApi.updateMemo(memoId, data);
      if (!res.success || !res.result) {
        set({ error: res.error?.message ?? 'Failed to update memo' });
        return null;
      }
      const updated = res.result;
      const memoKeys = Object.keys(get().memosBySchema);

      const effectiveSchemaId =
        schemaId ||
        memoKeys.find((sid) =>
          (get().memosBySchema[sid] ?? []).some((m) => m.id === memoId),
        );

      if (effectiveSchemaId) {
        set((state) => {
          const list = state.memosBySchema[effectiveSchemaId] ?? [];
          const next = list.map((m) => (m.id === memoId ? updated : m));
          return {
            memosBySchema: {
              ...state.memosBySchema,
              [effectiveSchemaId]: next,
            },
          };
        });
      }
      return updated;
    } catch (e) {
      set({ error: e instanceof Error ? e.message : 'Failed to update memo' });
      return null;
    }
  },

  deleteMemo: async (memoId: string, schemaId: string) => {
    set({ error: null });
    try {
      const res = await memoApi.deleteMemo(memoId);
      if (!res.success) {
        set({ error: res.error?.message ?? 'Failed to delete memo' });
        return false;
      }
      set((state) => {
        const list = state.memosBySchema[schemaId] ?? [];
        return {
          memosBySchema: {
            ...state.memosBySchema,
            [schemaId]: list.filter((m) => m.id !== memoId),
          },
        };
      });

      set((state) => {
        const next = { ...state.commentsByMemo };
        delete next[memoId];
        return { commentsByMemo: next };
      });
      return true;
    } catch (e) {
      set({ error: e instanceof Error ? e.message : 'Failed to delete memo' });
      return false;
    }
  },

  fetchMemoComments: async (memoId: string) => {
    set({ error: null });
    try {
      const res = await memoApi.getMemoComments(memoId);
      if (!res.success || !res.result) {
        set({ error: res.error?.message ?? 'Failed to fetch comments' });
        return;
      }
      set((state) => ({
        commentsByMemo: { ...state.commentsByMemo, [memoId]: res.result! },
      }));
    } catch (e) {
      set({
        error: e instanceof Error ? e.message : 'Failed to fetch comments',
      });
    }
  },

  createMemoComment: async (memoId: string, data: CreateMemoCommentRequest) => {
    set({ error: null });
    try {
      console.log('createMemoComment', memoId, data);
      const res = await memoApi.createMemoComment(memoId, data);
      if (!res.success || !res.result) {
        set({ error: res.error?.message ?? 'Failed to create comment' });
        return null;
      }
      const comment = res.result;
      set((state) => {
        const list = state.commentsByMemo[memoId] ?? [];
        return {
          commentsByMemo: {
            ...state.commentsByMemo,
            [memoId]: [...list, comment],
          },
        };
      });

      set((state) => {
        const schemaId = Object.keys(state.memosBySchema).find((sid) =>
          (state.memosBySchema[sid] ?? []).some((m) => m.id === memoId),
        );
        if (!schemaId) return {};
        const nextList = (state.memosBySchema[schemaId] ?? []).map((m) =>
          m.id === memoId
            ? { ...m, comments: [...(m.comments ?? []), comment] }
            : m,
        );
        return {
          memosBySchema: { ...state.memosBySchema, [schemaId]: nextList },
        };
      });
      return comment;
    } catch (e) {
      set({
        error: e instanceof Error ? e.message : 'Failed to create comment',
      });
      return null;
    }
  },

  updateMemoComment: async (
    memoId: string,
    commentId: string,
    data: UpdateMemoCommentRequest,
  ) => {
    set({ error: null });
    try {
      const res = await memoApi.updateMemoComment(memoId, commentId, data);
      if (!res.success || !res.result) {
        set({ error: res.error?.message ?? 'Failed to update comment' });
        return null;
      }
      const updated = res.result;
      set((state) => {
        const list = state.commentsByMemo[memoId] ?? [];
        const next = list.map((c) => (c.id === commentId ? updated : c));
        return { commentsByMemo: { ...state.commentsByMemo, [memoId]: next } };
      });
      set((state) => {
        const schemaId = Object.keys(state.memosBySchema).find((sid) =>
          (state.memosBySchema[sid] ?? []).some((m) => m.id === memoId),
        );
        if (!schemaId) return {};
        const nextList = (state.memosBySchema[schemaId] ?? []).map((m) =>
          m.id === memoId
            ? {
                ...m,
                comments: (m.comments ?? []).map((c) =>
                  c.id === commentId ? updated : c,
                ),
              }
            : m,
        );
        return {
          memosBySchema: { ...state.memosBySchema, [schemaId]: nextList },
        };
      });
      return updated;
    } catch (e) {
      set({
        error: e instanceof Error ? e.message : 'Failed to update comment',
      });
      return null;
    }
  },

  deleteMemoComment: async (memoId: string, commentId: string) => {
    set({ error: null });
    try {
      const res = await memoApi.deleteMemoComment(memoId, commentId);
      if (!res.success) {
        set({ error: res.error?.message ?? 'Failed to delete comment' });
        return false;
      }
      set((state) => {
        const list = state.commentsByMemo[memoId] ?? [];
        return {
          commentsByMemo: {
            ...state.commentsByMemo,
            [memoId]: list.filter((c) => c.id !== commentId),
          },
        };
      });
      set((state) => {
        const schemaId = Object.keys(state.memosBySchema).find((sid) =>
          (state.memosBySchema[sid] ?? []).some((m) => m.id === memoId),
        );
        if (!schemaId) return {};
        const nextList = (state.memosBySchema[schemaId] ?? []).reduce(
          (acc, m) => {
            if (m.id !== memoId) {
              acc.push(m);
              return acc;
            }
            const updatedComments = (m.comments ?? []).filter(
              (c) => c.id !== commentId,
            );
            if (updatedComments.length > 0) {
              acc.push({ ...m, comments: updatedComments });
            }
            return acc;
          },
          [] as Memo[],
        );
        return {
          memosBySchema: { ...state.memosBySchema, [schemaId]: nextList },
        };
      });
      return true;
    } catch (e) {
      set({
        error: e instanceof Error ? e.message : 'Failed to delete comment',
      });
      return false;
    }
  },

  clearError: () => set({ error: null }),
}));
