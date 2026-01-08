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
import { handleAsync, type AsyncHandlerContext } from './helpers';

type MemosBySchema = Record<string, Memo[]>;
type CommentsByMemo = Record<string, MemoComment[]>;

export class MemoStore implements AsyncHandlerContext {
  private static instance: MemoStore;

  memosBySchema: MemosBySchema = {};
  commentsByMemo: CommentsByMemo = {};

  _loadingStates: Record<string, boolean> = {};
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

  isLoading(operation: string): boolean {
    return !!this._loadingStates[operation];
  }

  async fetchSchemaMemos(schemaId: string) {
    this._loadingStates['fetchSchemaMemos'] = true;
    this.error = null;
    try {
      const res = await memoApi.getSchemaMemos(schemaId);
      if (!res.success || !res.result) {
        runInAction(() => {
          this.error = res.error?.message ?? 'Failed to fetch memos';
          this._loadingStates['fetchSchemaMemos'] = false;
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
          acc[memo.id] = memo.comments;
          return acc;
        }, {} as CommentsByMemo);

        this.memosBySchema[schemaId] = combinedMemos;
        this.commentsByMemo = {
          ...this.commentsByMemo,
          ...newCommentsByMemo,
        };
        this._loadingStates['fetchSchemaMemos'] = false;
      });
    } catch (e) {
      runInAction(() => {
        this._loadingStates['fetchSchemaMemos'] = false;
        this.error = e instanceof Error ? e.message : 'Failed to fetch memos';
      });
    }
  }

  async createMemo(data: CreateMemoRequest): Promise<Memo | null> {
    const { data: memo } = await handleAsync(
      this,
      'createMemo',
      () => memoApi.createMemo(data),
      (memo) => {
        const list = this.memosBySchema[data.schemaId] ?? [];
        this.memosBySchema[data.schemaId] = [memo, ...list];
      },
      'Failed to create memo',
    );
    return memo;
  }

  async updateMemo(
    memoId: string,
    data: UpdateMemoRequest,
    schemaId?: string,
  ): Promise<Memo | null> {
    const { data: updated } = await handleAsync(
      this,
      'updateMemo',
      () => memoApi.updateMemo(memoId, data),
      (updated) => {
        const memoKeys = Object.keys(this.memosBySchema);
        const effectiveSchemaId =
          schemaId ||
          memoKeys.find((sid) =>
            (this.memosBySchema[sid] ?? []).some((m) => m.id === memoId),
          );

        if (effectiveSchemaId) {
          const list = this.memosBySchema[effectiveSchemaId] ?? [];
          this.memosBySchema[effectiveSchemaId] = list.map((m) =>
            m.id === memoId ? { ...updated, comments: m.comments } : m,
          );
        }
      },
      'Failed to update memo',
    );
    return updated;
  }

  async deleteMemo(memoId: string, schemaId: string): Promise<boolean> {
    const { success } = await handleAsync(
      this,
      'deleteMemo',
      () => memoApi.deleteMemo(memoId),
      () => {
        const list = this.memosBySchema[schemaId] ?? [];
        this.memosBySchema[schemaId] = list.filter((m) => m.id !== memoId);

        const nextCommentsByMemo = { ...this.commentsByMemo };
        delete nextCommentsByMemo[memoId];
        this.commentsByMemo = nextCommentsByMemo;
      },
      'Failed to delete memo',
    );
    return success;
  }

  async fetchMemoComments(memoId: string) {
    await handleAsync(
      this,
      'fetchMemoComments',
      () => memoApi.getMemoComments(memoId),
      (comments) => {
        this.commentsByMemo[memoId] = comments;
      },
      'Failed to fetch comments',
    );
  }

  async createMemoComment(
    memoId: string,
    data: CreateMemoCommentRequest,
  ): Promise<MemoComment | null> {
    const { data: comment } = await handleAsync(
      this,
      'createMemoComment',
      () => memoApi.createMemoComment(memoId, data),
      (comment) => {
        const list = this.commentsByMemo[memoId] ?? [];
        this.commentsByMemo[memoId] = [...list, comment];

        const schemaId = Object.keys(this.memosBySchema).find((sid) =>
          (this.memosBySchema[sid] ?? []).some((m) => m.id === memoId),
        );

        if (schemaId) {
          const listMemos = this.memosBySchema[schemaId] ?? [];
          this.memosBySchema[schemaId] = listMemos.map((m) =>
            m.id === memoId ? { ...m, comments: [...m.comments, comment] } : m,
          );
        }
      },
      'Failed to create comment',
    );
    return comment;
  }

  async updateMemoComment(
    memoId: string,
    commentId: string,
    data: UpdateMemoCommentRequest,
  ): Promise<MemoComment | null> {
    const { data: updated } = await handleAsync(
      this,
      'updateMemoComment',
      () => memoApi.updateMemoComment(memoId, commentId, data),
      (updated) => {
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
                  comments: m.comments.map((c) =>
                    c.id === commentId ? updated : c,
                  ),
                }
              : m,
          );
        }
      },
      'Failed to update comment',
    );
    return updated;
  }

  async deleteMemoComment(memoId: string, commentId: string): Promise<boolean> {
    const { success } = await handleAsync(
      this,
      'deleteMemoComment',
      () => memoApi.deleteMemoComment(memoId, commentId),
      () => {
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
            const updatedComments = m.comments.filter(
              (c) => c.id !== commentId,
            );
            acc.push({ ...m, comments: updatedComments });
            return acc;
          }, [] as Memo[]);
        }
      },
      'Failed to delete comment',
    );
    return success;
  }

  clearError() {
    this.error = null;
  }
}
