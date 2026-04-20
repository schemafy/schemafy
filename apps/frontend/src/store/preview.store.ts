import { makeObservable, observable, action, runInAction } from 'mobx';
import type { PreviewEntry } from './preview.types';

const SWEEP_INTERVAL_MS = 30_000;

export class PreviewStore {
  previews: Map<string, PreviewEntry> = new Map();
  private timers: Map<string, ReturnType<typeof setTimeout>> = new Map();
  private sweepIntervalId: ReturnType<typeof setInterval> | null = null;

  constructor() {
    makeObservable(this, {
      previews: observable,
      addPreview: action,
      removePreview: action,
      clearBySchema: action,
      clearBySession: action,
      clearAll: action,
    });

    this.sweepIntervalId = setInterval(() => this.sweep(), SWEEP_INTERVAL_MS);
  }

  addPreview(entry: PreviewEntry) {
    this.previews.set(entry.previewId, entry);

    if (entry.ttlMs !== null) {
      this.scheduleTtl(entry.previewId, entry.ttlMs);
    }
  }

  removePreview(previewId: string) {
    this.clearTimer(previewId);
    this.previews.delete(previewId);
  }

  clearBySchema(schemaId: string) {
    for (const [id, entry] of this.previews) {
      if (entry.schemaId === schemaId) {
        this.clearTimer(id);
        this.previews.delete(id);
      }
    }
  }

  clearBySession(sessionId: string) {
    for (const [id, entry] of this.previews) {
      if (entry.sessionId === sessionId) {
        this.clearTimer(id);
        this.previews.delete(id);
      }
    }
  }

  clearAll() {
    for (const id of this.timers.keys()) {
      this.clearTimer(id);
    }
    this.previews.clear();
  }

  private scheduleTtl(previewId: string, ttlMs: number) {
    this.clearTimer(previewId);
    const timer = setTimeout(() => {
      runInAction(() => {
        this.previews.delete(previewId);
      });
      this.timers.delete(previewId);
    }, ttlMs);
    this.timers.set(previewId, timer);
  }

  private clearTimer(previewId: string) {
    const timer = this.timers.get(previewId);
    if (timer !== undefined) {
      clearTimeout(timer);
      this.timers.delete(previewId);
    }
  }

  private sweep() {
    const now = Date.now();
    runInAction(() => {
      for (const [id, entry] of this.previews) {
        if (entry.ttlMs !== null && now - entry.createdAt >= entry.ttlMs) {
          this.clearTimer(id);
          this.previews.delete(id);
        }
      }
    });
  }
}

export const previewStore = new PreviewStore();
