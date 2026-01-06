import { makeAutoObservable, runInAction } from 'mobx';
import {
  getWorkspaces,
  getWorkspace,
  createWorkspace,
  getProjects,
  getProject,
  createProject,
} from '../lib/api';
import type {
  Workspace,
  WorkspacesResponse,
  WorkspaceRequest,
} from '../lib/api/workspace/types';
import type {
  Project,
  ProjectsResponse,
  ProjectRequest,
} from '../lib/api/project/types';
import type { ApiResponse } from '../lib/api/types';

export class ProjectStore {
  private static instance: ProjectStore;

  workspaces: WorkspacesResponse | null = null;
  currentWorkspace: Workspace | null = null;
  projects: ProjectsResponse | null = null;
  currentProject: Project | null = null;

  private _loadingStates: Record<string, boolean> = {};

  error: string | null = null;

  private constructor() {
    makeAutoObservable(this);
  }

  static getInstance(): ProjectStore {
    if (!ProjectStore.instance) {
      ProjectStore.instance = new ProjectStore();
    }
    return ProjectStore.instance;
  }

  isLoading(operation: string): boolean {
    return !!this._loadingStates[operation];
  }

  private async handleAsync<T>(
    operation: string,
    apiCall: () => Promise<ApiResponse<T>>,
    onSuccess: (result: T) => void,
    defaultErrorMessage: string,
  ): Promise<{ success: boolean; data: T | null }> {
    this._loadingStates[operation] = true;
    this.error = null;

    try {
      const res = await apiCall();
      if (!res.success) {
        runInAction(() => {
          this.error = res.error?.message ?? defaultErrorMessage;
          this._loadingStates[operation] = false;
        });
        return { success: false, data: null };
      }

      runInAction(() => {
        onSuccess(res.result as T);
        this._loadingStates[operation] = false;
      });

      return { success: true, data: res.result as T };
    } catch (e) {
      runInAction(() => {
        this.error = e instanceof Error ? e.message : defaultErrorMessage;
        this._loadingStates[operation] = false;
      });
      return { success: false, data: null };
    }
  }

  async fetchWorkspaces(page: number = 0, size: number = 5) {
    await this.handleAsync(
      'fetchWorkspaces',
      () => getWorkspaces(page, size),
      (result) => {
        this.workspaces = result;
      },
      'Failed to fetch workspaces',
    );
  }

  async fetchWorkspace(workspaceId: string) {
    await this.handleAsync(
      'fetchWorkspace',
      () => getWorkspace(workspaceId),
      (result) => {
        this.currentWorkspace = result;
      },
      'Failed to fetch workspace',
    );
  }

  async fetchProjects(workspaceId: string, page: number = 0, size: number = 5) {
    await this.handleAsync(
      'fetchProjects',
      () => getProjects(workspaceId, page, size),
      (result) => {
        this.projects = result;
      },
      'Failed to fetch projects',
    );
  }

  async fetchProject(workspaceId: string, projectId: string) {
    await this.handleAsync(
      'fetchProject',
      () => getProject(workspaceId, projectId),
      (result) => {
        this.currentProject = result;
      },
      'Failed to fetch project',
    );
  }

  async createWorkspace(data: WorkspaceRequest): Promise<Workspace | null> {
    const { data: workspace } = await this.handleAsync(
      'createWorkspace',
      () => createWorkspace(data),
      (workspace) => {
        if (this.workspaces) {
          this.workspaces = {
            ...this.workspaces,
            content: [{ ...workspace, memberCount: 1 }, ...this.workspaces.content],
            totalElements: this.workspaces.totalElements + 1,
          };
        }
      },
      'Failed to create workspace',
    );
    return workspace;
  }

  async createProject(workspaceId: string, data: ProjectRequest): Promise<Project | null> {
    const { data: project } = await this.handleAsync(
      'createProject',
      () => createProject(workspaceId, data),
      (project) => {
        if (this.projects) {
          this.projects = {
            ...this.projects,
            content: [
              {
                ...project,
                myRole: 'OWNER',
                memberCount: 1,
              },
              ...this.projects.content,
            ],
            totalElements: this.projects.totalElements + 1,
          };
        }
      },
      'Failed to create project',
    );
    return project;
  }

  clearWorkspaces() {
    this.workspaces = null;
  }

  clearCurrentWorkspace() {
    this.currentWorkspace = null;
  }

  clearProjects() {
    this.projects = null;
  }

  clearCurrentProject() {
    this.currentProject = null;
  }

  clearAll() {
    this.workspaces = null;
    this.currentWorkspace = null;
    this.projects = null;
    this.currentProject = null;
    this.error = null;
  }
}
