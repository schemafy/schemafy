import { makeAutoObservable, runInAction } from 'mobx';
import {
  getWorkspaces,
  getWorkspace,
  createWorkspace,
  getProjects,
  getProject,
  createProject,
  deleteProject,
  deleteWorkspace,
  updateWorkspace,
  updateProject,
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
    console.log(data);
    const { data: workspace } = await this.handleAsync(
      'createWorkspace',
      () => createWorkspace(data),
      (workspace) => {
        if (this.workspaces) {
          const fetchContents = [
            { ...workspace, memberCount: 1 },
            ...this.workspaces.content,
          ];
          this.workspaces = {
            ...this.workspaces,
            content: fetchContents.sort((a, b) => a.name.localeCompare(b.name)),
            totalElements: this.workspaces.totalElements + 1,
          };
        } else {
          this.workspaces = {
            page: 1,
            size: 1,
            totalElements: 1,
            totalPages: 1,
            content: [{ ...workspace, memberCount: 1 }],
          };
        }
      },
      'Failed to create workspace',
    );
    return workspace;
  }

  async createProject(
    workspaceId: string,
    data: ProjectRequest,
  ): Promise<Project | null> {
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
        } else {
          this.projects = {
            page: 1,
            size: 1,
            totalElements: 1,
            totalPages: 1,
            content: [
              {
                ...project,
                myRole: 'OWNER',
                memberCount: 1,
              },
            ],
          };
        }
      },
      'Failed to create project',
    );
    return project;
  }

  async updateWorkspace(workspaceID: string, data: WorkspaceRequest) {
    await this.handleAsync(
      'editWorkspace',
      () => updateWorkspace(workspaceID, data),
      (workspace) => {
        if (this.workspaces) {
          this.workspaces = {
            ...this.workspaces,
            content: this.workspaces.content.map((w) =>
              w.id === workspaceID
                ? { ...workspace, memberCount: w.memberCount }
                : w,
            ),
          };

          if (this.currentWorkspace?.id == workspaceID) {
            this.currentWorkspace = workspace;
          }
        }
      },
      'Failed to edit workspace',
    );
  }

  async updateProject(
    workspaceId: string,
    projectId: string,
    data: ProjectRequest,
  ) {
    await this.handleAsync(
      'editProject',
      () => updateProject(workspaceId, projectId, data),
      (project) => {
        if (this.projects) {
          this.projects = {
            ...this.projects,
            content: this.projects.content.map((p) =>
              p.id === projectId
                ? { ...project, memberCount: p.memberCount, myRole: p.myRole }
                : p,
            ),
          };
        }
      },
      'Failed to edit project',
    );
  }

  async editProject(
    workspaceId: string,
    projectId: string,
    data: ProjectRequest,
  ) {
    await this.handleAsync(
      'editProject',
      () => updateProject(workspaceId, projectId, data),
      (project) => {
        if (this.projects) {
          this.projects = {
            ...this.projects,
            content: this.projects.content.map((p) =>
              p.id === projectId
                ? { ...project, memberCount: p.memberCount, myRole: p.myRole }
                : p,
            ),
          };
        }
      },
      'Failed to edit project',
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

  async deleteProject(
    workspaceId: string,
    projectId: string,
  ): Promise<boolean> {
    this._loadingStates['deleteProject'] = true;
    this.error = null;

    try {
      await deleteProject(workspaceId, projectId);

      runInAction(() => {
        if (this.projects) {
          this.projects = {
            ...this.projects,
            content: this.projects.content.filter((p) => p.id !== projectId),
            totalElements: this.projects.totalElements - 1,
          };
        }
        this._loadingStates['deleteProject'] = false;
      });

      return true;
    } catch (e) {
      runInAction(() => {
        this.error =
          e instanceof Error ? e.message : 'Failed to delete project';
        this._loadingStates['deleteProject'] = false;
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
