import { makeAutoObservable, runInAction } from 'mobx';
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

export class MemoStore {
  private static instance: MemoStore;

  memosBySchema: MemosBySchema = {};
  commentsByMemo: CommentsByMemo = {};
  isLoading: boolean = false;
  error: string | null = null;

  private constructor() {
    makeAutoObservable(this);
  }

  static getInstance(): MemoStore {
    if (!MemoStore.instance) {
      MemoStore.instance = new MemoStore();
    }
    return MemoStore.instance;
  }

  async fetchSchemaMemos(schemaId: string) {
    this.isLoading = true;
    this.error = null;
    try {
      const res = await memoApi.getSchemaMemos(schemaId);
      if (!res.success || !res.result) {
        runInAction(() => {
          this.error = res.error?.message ?? 'Failed to fetch memos';
          this.isLoading = false;
        });
        return;
      }
      const memos = res.result;

      const commentsResults = await Promise.allSettled(
        memos.map((memo) => memoApi.getMemoComments(memo.id)),
      );

      runInAction(() => {
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

        this.memosBySchema[schemaId] = combinedMemos;
        this.commentsByMemo = {
          ...this.commentsByMemo,
          ...newCommentsByMemo,
        };
        this.isLoading = false;
      });
    } catch (e) {
      runInAction(() => {
        this.isLoading = false;
        this.error = e instanceof Error ? e.message : 'Failed to fetch memos';
      });
    }
  }

  async createMemo(data: CreateMemoRequest): Promise<Memo | null> {
    this.error = null;
    try {
      const res = await memoApi.createMemo(data);
      if (!res.success || !res.result) {
        runInAction(() => {
          this.error = res.error?.message ?? 'Failed to create memo';
          this.isLoading = false;
        });
        return null;
      }
      const memo = res.result;
      runInAction(() => {
        const list = this.memosBySchema[data.schemaId] ?? [];
        this.memosBySchema[data.schemaId] = [memo, ...list];
      });
      return memo;
    } catch (e) {
      runInAction(() => {
        this.error = e instanceof Error ? e.message : 'Failed to create memo';
        this.isLoading = false;
      });
      return null;
    }
  }

  async updateMemo(
    memoId: string,
    data: UpdateMemoRequest,
    schemaId?: string,
  ): Promise<Memo | null> {
    this.error = null;
    try {
      const res = await memoApi.updateMemo(memoId, data);
      if (!res.success || !res.result) {
        runInAction(() => {
          this.error = res.error?.message ?? 'Failed to update memo';
          this.isLoading = false;
        });
        return null;
      }
      const updated = res.result;
      
      runInAction(() => {
        const memoKeys = Object.keys(this.memosBySchema);
        const effectiveSchemaId =
          schemaId ||
          memoKeys.find((sid) =>
            (this.memosBySchema[sid] ?? []).some((m) => m.id === memoId),
          );

        if (effectiveSchemaId) {
          const list = this.memosBySchema[effectiveSchemaId] ?? [];
          this.memosBySchema[effectiveSchemaId] = list.map((m) =>
            m.id === memoId ? updated : m,
          );
        }
      });
      return updated;
    } catch (e) {
      runInAction(() => {
        this.error = e instanceof Error ? e.message : 'Failed to update memo';
        this.isLoading = false;
      });
      return null;
    }
  }

  async deleteMemo(memoId: string, schemaId: string): Promise<boolean> {
    this.error = null;
    try {
      const res = await memoApi.deleteMemo(memoId);
      if (!res.success) {
        runInAction(() => {
          this.error = res.error?.message ?? 'Failed to delete memo';
          this.isLoading = false;
        });
        return false;
      }
      runInAction(() => {
        const list = this.memosBySchema[schemaId] ?? [];
        this.memosBySchema[schemaId] = list.filter((m) => m.id !== memoId);
        
        const nextCommentsByMemo = { ...this.commentsByMemo };
        delete nextCommentsByMemo[memoId];
        this.commentsByMemo = nextCommentsByMemo;
      });
      return true;
    } catch (e) {
      runInAction(() => {
        this.error = e instanceof Error ? e.message : 'Failed to delete memo';
        this.isLoading = false;
      });
      return false;
    }
  }

  async fetchMemoComments(memoId: string) {
    this.error = null;
    try {
      const res = await memoApi.getMemoComments(memoId);
      if (!res.success || !res.result) {
        runInAction(() => {
          this.error = res.error?.message ?? 'Failed to fetch comments';
          this.isLoading = false;
        });
        return;
      }
      runInAction(() => {
        this.commentsByMemo[memoId] = res.result!;
      });
    } catch (e) {
      runInAction(() => {
        this.error = e instanceof Error ? e.message : 'Failed to fetch comments';
        this.isLoading = false;
      });
    }
  }

  async createMemoComment(
    memoId: string,
    data: CreateMemoCommentRequest,
  ): Promise<MemoComment | null> {
    this.error = null;
    try {
      const res = await memoApi.createMemoComment(memoId, data);
      if (!res.success || !res.result) {
        runInAction(() => {
          this.error = res.error?.message ?? 'Failed to create comment';
          this.isLoading = false;
        });
        return null;
      }
      const comment = res.result;

      runInAction(() => {
        const list = this.commentsByMemo[memoId] ?? [];
        this.commentsByMemo[memoId] = [...list, comment];

        const schemaId = Object.keys(this.memosBySchema).find((sid) =>
          (this.memosBySchema[sid] ?? []).some((m) => m.id === memoId),
        );

        if (schemaId) {
          const listMemos = this.memosBySchema[schemaId] ?? [];
          this.memosBySchema[schemaId] = listMemos.map((m) =>
            m.id === memoId
              ? { ...m, comments: [...(m.comments ?? []), comment] }
              : m,
          );
        }
      });
      return comment;
    } catch (e) {
      runInAction(() => {
        this.error = e instanceof Error ? e.message : 'Failed to create comment';
        this.isLoading = false;
      });
      return null;
    }
  }

  async updateMemoComment(
    memoId: string,
    commentId: string,
    data: UpdateMemoCommentRequest,
  ): Promise<MemoComment | null> {
    this.error = null;
    try {
      const res = await memoApi.updateMemoComment(memoId, commentId, data);
      if (!res.success || !res.result) {
        runInAction(() => {
          this.error = res.error?.message ?? 'Failed to update comment';
          this.isLoading = false;
        });
        return null;
      }
      const updated = res.result;

      runInAction(() => {
        const list = this.commentsByMemo[memoId] ?? [];
        this.commentsByMemo[memoId] = list.map((c) =>
          c.id === commentId ? updated : c,
        );

        const schemaId = Object.keys(this.memosBySchema).find((sid) =>
          (this.memosBySchema[sid] ?? []).some((m) => m.id === memoId),
        );

        if (schemaId) {
          const listMemos = this.memosBySchema[schemaId] ?? [];
          this.memosBySchema[schemaId] = listMemos.map((m) =>
            m.id === memoId
              ? {
                  ...m,
                  comments: (m.comments ?? []).map((c) =>
                    c.id === commentId ? updated : c,
                  ),
                }
              : m,
          );
        }
      });
      return updated;
    } catch (e) {
      runInAction(() => {
        this.error = e instanceof Error ? e.message : 'Failed to update comment';
        this.isLoading = false;
      });
      return null;
    }
  }

  async deleteMemoComment(memoId: string, commentId: string): Promise<boolean> {
    this.error = null;
    try {
      const res = await memoApi.deleteMemoComment(memoId, commentId);
      if (!res.success) {
        runInAction(() => {
          this.error = res.error?.message ?? 'Failed to delete comment';
          this.isLoading = false;
        });
        return false;
      }

      runInAction(() => {
        const list = this.commentsByMemo[memoId] ?? [];
        this.commentsByMemo[memoId] = list.filter((c) => c.id !== commentId);

        const schemaId = Object.keys(this.memosBySchema).find((sid) =>
          (this.memosBySchema[sid] ?? []).some((m) => m.id === memoId),
        );

        if (schemaId) {
          const listMemos = this.memosBySchema[schemaId] ?? [];
          this.memosBySchema[schemaId] = listMemos.reduce((acc, m) => {
            if (m.id !== memoId) {
              acc.push(m);
              return acc;
            }
            const updatedComments = (m.comments ?? []).filter(
              (c) => c.id !== commentId,
            );
            acc.push({ ...m, comments: updatedComments });
            return acc;
          }, [] as Memo[]);
        }
      });
      return true;
    } catch (e) {
      runInAction(() => {
        this.error = e instanceof Error ? e.message : 'Failed to delete comment';
        this.isLoading = false;
      });
      return false;
    }
  }

  clearError() {
    this.error = null;
  }
}
