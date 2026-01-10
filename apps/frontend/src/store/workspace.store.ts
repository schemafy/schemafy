import { makeAutoObservable, runInAction } from 'mobx';
import {
  getWorkspaces,
  getWorkspace,
  createWorkspace,
  deleteWorkspace,
  updateWorkspace,
} from '@/lib/api';
import type {
  Workspace,
  WorkspacesResponse,
  WorkspaceRequest,
} from '@/lib/api/workspace/types';
import { handleAsync, type AsyncHandlerContext } from './helpers';

export class WorkspaceStore implements AsyncHandlerContext {
  private static instance: WorkspaceStore;

  workspaces: WorkspacesResponse | null = null;
  currentWorkspace: Workspace | null = null;

  _loadingStates: Record<string, boolean> = {};
  error: string | null = null;

  private constructor() {
    makeAutoObservable(this);
  }

  static getInstance(): WorkspaceStore {
    if (!WorkspaceStore.instance) {
      WorkspaceStore.instance = new WorkspaceStore();
    }
    return WorkspaceStore.instance;
  }

  isLoading(operation: string): boolean {
    return !!this._loadingStates[operation];
  }

  async fetchWorkspaces(page: number = 0, size: number = 5) {
    await handleAsync(
      this,
      'fetchWorkspaces',
      () => getWorkspaces(page, size),
      (result) => {
        const sortedContent = [...result.content].sort((a, b) =>
          a.name.localeCompare(b.name),
        );
        this.workspaces = { ...result, content: sortedContent };
        this.currentWorkspace = sortedContent[0];
      },
      'Failed to fetch workspaces',
    );
  }

  setWorkspace(workspace: Workspace) {
    this.currentWorkspace = workspace;
  }

  async fetchWorkspace(workspaceId: string) {
    await handleAsync(
      this,
      'fetchWorkspace',
      () => getWorkspace(workspaceId),
      (result) => {
        this.currentWorkspace = result;
      },
      'Failed to fetch workspace',
    );
  }

  async createWorkspace(data: WorkspaceRequest): Promise<Workspace | null> {
    const { data: workspace } = await handleAsync(
      this,
      'createWorkspace',
      () => createWorkspace(data),
      (workspace) => {
        const existing = this.workspaces ?? {
          page: 1,
          size: 10,
          totalElements: 0,
          totalPages: 1,
          content: [],
        };
        const newItem = { ...workspace, memberCount: 1 };
        const newContent = [newItem, ...existing.content].sort((a, b) =>
          a.name.localeCompare(b.name),
        );
        this.workspaces = {
          ...existing,
          content: newContent,
          totalElements: existing.totalElements + 1,
        };
      },
      'Failed to create workspace',
    );
    return workspace;
  }

  async updateWorkspace(workspaceId: string, data: WorkspaceRequest) {
    await handleAsync(
      this,
      'updateWorkspace',
      () => updateWorkspace(workspaceId, data),
      (workspace) => {
        if (this.workspaces) {
          this.workspaces = {
            ...this.workspaces,
            content: this.workspaces.content.map((w) =>
              w.id === workspaceId
                ? { ...workspace, memberCount: w.memberCount }
                : w,
            ),
          };

          if (this.currentWorkspace?.id === workspaceId) {
            this.currentWorkspace = workspace;
          }
        }
      },
      'Failed to update workspace',
    );
  }

  async deleteWorkspace(workspaceId: string) {
    this._loadingStates['deleteWorkspace'] = true;
    this.error = null;

    try {
      await deleteWorkspace(workspaceId);

      runInAction(() => {
        if (this.workspaces) {
          this.workspaces = {
            ...this.workspaces,
            content: this.workspaces.content.filter(
              (w) => w.id !== workspaceId,
            ),
            totalElements: this.workspaces.totalElements - 1,
          };
        }
        if (this.currentWorkspace?.id === workspaceId) {
          this.currentWorkspace = null;
        }
        this._loadingStates['deleteWorkspace'] = false;
      });

      return true;
    } catch (e) {
      runInAction(() => {
        this.error =
          e instanceof Error ? e.message : 'Failed to delete workspace';
        this._loadingStates['deleteWorkspace'] = false;
      });
      return false;
    }
  }

  clearWorkspaces() {
    this.workspaces = null;
  }

  clearCurrentWorkspace() {
    this.currentWorkspace = null;
  }

  clearAll() {
    this.workspaces = null;
    this.currentWorkspace = null;
    this.error = null;
  }
}
