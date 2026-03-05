import type { ErdCommand } from '@/features/drawing/history';
import type { Memo, MemoComment, CreateMemoRequest } from '../api/types';
import * as memoApi from '../api/api';

type SetStoredMemos = React.Dispatch<React.SetStateAction<Memo[]>>;

const removeMemo = (set: SetStoredMemos, memoId: string) =>
  set((prev) => prev.filter((m) => m.id !== memoId));

const addMemo = (set: SetStoredMemos, memo: Memo) =>
  set((prev) => [memo, ...prev]);

const updateMemoInCache = (set: SetStoredMemos, memoId: string, updated: Memo) =>
  set((prev) =>
    prev.map((m) => (m.id === memoId ? { ...updated, comments: m.comments } : m)),
  );

const removeComment = (set: SetStoredMemos, memoId: string, commentId: string) =>
  set((prev) =>
    prev.map((m) =>
      m.id === memoId
        ? { ...m, comments: m.comments.filter((c) => c.id !== commentId) }
        : m,
    ),
  );

const addComment = (set: SetStoredMemos, memoId: string, comment: MemoComment) =>
  set((prev) =>
    prev.map((m) =>
      m.id === memoId ? { ...m, comments: [...m.comments, comment] } : m,
    ),
  );

const replaceComment = (
  set: SetStoredMemos,
  memoId: string,
  commentId: string,
  updated: MemoComment,
) =>
  set((prev) =>
    prev.map((m) =>
      m.id === memoId
        ? { ...m, comments: m.comments.map((c) => (c.id === commentId ? updated : c)) }
        : m,
    ),
  );

export class CreateMemoCommand implements ErdCommand {
  private currentMemoId: string;

  constructor(
    private params: {
      memoId: string;
      originalRequest: CreateMemoRequest;
      setStoredMemos: SetStoredMemos;
    },
  ) {
    this.currentMemoId = params.memoId;
  }

  async undo(): Promise<void> {
    await memoApi.deleteMemo(this.currentMemoId);
    removeMemo(this.params.setStoredMemos, this.currentMemoId);
  }

  async redo(): Promise<void> {
    const memo = await memoApi.createMemo(this.params.originalRequest);
    this.currentMemoId = memo.id;
    addMemo(this.params.setStoredMemos, memo);
  }
}

export class DeleteMemoCommand implements ErdCommand {
  private restoredMemoId: string | null = null;

  constructor(
    private params: {
      memo: Memo;
      setStoredMemos: SetStoredMemos;
    },
  ) {}

  async undo(): Promise<void> {
    const { memo } = this.params;
    const created = await memoApi.createMemo({
      schemaId: memo.schemaId,
      positions: memo.positions,
      body: '',
    });

    const restoredComments: MemoComment[] = [];
    for (const comment of memo.comments) {
      const newComment = await memoApi.createMemoComment(created.id, {
        body: comment.body,
      });
      restoredComments.push(newComment);
    }

    this.restoredMemoId = created.id;
    addMemo(this.params.setStoredMemos, { ...created, comments: restoredComments });
  }

  async redo(): Promise<void> {
    if (!this.restoredMemoId) return;
    const idToRemove = this.restoredMemoId;
    await memoApi.deleteMemo(idToRemove);
    this.restoredMemoId = null;
    removeMemo(this.params.setStoredMemos, idToRemove);
  }
}

export class MoveMemoCommand implements ErdCommand {
  constructor(
    private params: {
      memoId: string;
      previousPositions: string;
      newPositions: string;
      setStoredMemos: SetStoredMemos;
    },
  ) {}

  private async applyPositions(positions: string): Promise<void> {
    const updated = await memoApi.updateMemo(this.params.memoId, { positions });
    updateMemoInCache(this.params.setStoredMemos, this.params.memoId, updated);
  }

  async undo(): Promise<void> {
    await this.applyPositions(this.params.previousPositions);
  }

  async redo(): Promise<void> {
    await this.applyPositions(this.params.newPositions);
  }

  merge(other: ErdCommand): ErdCommand | null {
    if (!(other instanceof MoveMemoCommand)) return null;
    if (other.params.memoId !== this.params.memoId) return null;
    return new MoveMemoCommand({ ...this.params, newPositions: other.params.newPositions });
  }
}

export class CreateCommentCommand implements ErdCommand {
  private currentCommentId: string;

  constructor(
    private params: {
      memoId: string;
      commentId: string;
      originalBody: string;
      setStoredMemos: SetStoredMemos;
    },
  ) {
    this.currentCommentId = params.commentId;
  }

  async undo(): Promise<void> {
    await memoApi.deleteMemoComment(this.params.memoId, this.currentCommentId);
    removeComment(this.params.setStoredMemos, this.params.memoId, this.currentCommentId);
  }

  async redo(): Promise<void> {
    const comment = await memoApi.createMemoComment(this.params.memoId, {
      body: this.params.originalBody,
    });
    this.currentCommentId = comment.id;
    addComment(this.params.setStoredMemos, this.params.memoId, comment);
  }
}

export class DeleteCommentCommand implements ErdCommand {
  private restoredCommentId: string | null = null;

  constructor(
    private params: {
      memoId: string;
      comment: MemoComment;
      setStoredMemos: SetStoredMemos;
    },
  ) {}

  async undo(): Promise<void> {
    const newComment = await memoApi.createMemoComment(this.params.memoId, {
      body: this.params.comment.body,
    });
    this.restoredCommentId = newComment.id;
    addComment(this.params.setStoredMemos, this.params.memoId, newComment);
  }

  async redo(): Promise<void> {
    if (!this.restoredCommentId) return;
    const idToRemove = this.restoredCommentId;
    await memoApi.deleteMemoComment(this.params.memoId, idToRemove);
    this.restoredCommentId = null;
    removeComment(this.params.setStoredMemos, this.params.memoId, idToRemove);
  }
}

export class UpdateCommentCommand implements ErdCommand {
  constructor(
    private params: {
      memoId: string;
      commentId: string;
      previousBody: string;
      newBody: string;
      setStoredMemos: SetStoredMemos;
    },
  ) {}

  private async applyBody(body: string): Promise<void> {
    const updated = await memoApi.updateMemoComment(
      this.params.memoId,
      this.params.commentId,
      { body },
    );
    replaceComment(
      this.params.setStoredMemos,
      this.params.memoId,
      this.params.commentId,
      updated,
    );
  }

  async undo(): Promise<void> {
    await this.applyBody(this.params.previousBody);
  }

  async redo(): Promise<void> {
    await this.applyBody(this.params.newBody);
  }
}