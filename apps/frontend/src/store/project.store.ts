import { makeAutoObservable, runInAction } from 'mobx';
import {
  getProjects,
  getProject,
  createProject,
  deleteProject,
  updateProject,
} from '@/lib/api';
import type {
  Project,
  ProjectsResponse,
  ProjectRequest,
} from '@/lib/api/project/types';
import { handleAsync, type AsyncHandlerContext } from './helpers';

export class ProjectStore implements AsyncHandlerContext {
  private static instance: ProjectStore;

  projects: ProjectsResponse | null = null;
  currentProject: Project | null = null;

  _loadingStates: Record<string, boolean> = {};
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

  async fetchProjects(workspaceId: string, page: number = 0, size: number = 5) {
    await handleAsync(
      this,
      'fetchProjects',
      () => getProjects(workspaceId, page, size),
      (result) => {
        this.projects = result;
      },
      'Failed to fetch projects',
    );
  }

  async fetchProject(workspaceId: string, projectId: string) {
    await handleAsync(
      this,
      'fetchProject',
      () => getProject(workspaceId, projectId),
      (result) => {
        this.currentProject = result;
      },
      'Failed to fetch project',
    );
  }

  async createProject(
    workspaceId: string,
    data: ProjectRequest,
  ): Promise<Project | null> {
    const { data: project } = await handleAsync(
      this,
      'createProject',
      () => createProject(workspaceId, data),
      (project) => {
        const existing = this.projects ?? {
          page: 1,
          size: 10,
          totalElements: 0,
          totalPages: 1,
          content: [],
        };
        const newItem = {
          ...project,
          myRole: 'OWNER' as const,
          memberCount: 1,
        };
        this.projects = {
          ...existing,
          content: [newItem, ...existing.content],
          totalElements: existing.totalElements + 1,
        };
      },
      'Failed to create project',
    );
    return project;
  }

  async updateProject(
    workspaceId: string,
    projectId: string,
    data: ProjectRequest,
  ) {
    await handleAsync(
      this,
      'updateProject',
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
      'Failed to update project',
    );
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

  clearProjects() {
    this.projects = null;
  }

  clearCurrentProject() {
    this.currentProject = null;
  }

  clearAll() {
    this.projects = null;
    this.currentProject = null;
    this.error = null;
  }
}
