import { useEffect, useCallback } from 'react';
import { ProjectStore } from '@/store';
import type { ProjectRequest, WorkspaceRequest, Workspace } from '@/lib/api';

export const useProjectStore = () => {
  const store = ProjectStore.getInstance();

  const workspaces = store.workspaces?.content ?? [];
  const currentWorkspace = store.currentWorkspace;
  const projects = store.projects?.content ?? [];
  const totalPages = store.projects?.totalPages ?? 1;
  const isLoading = store.isLoading('fetchProjects');

  const setWorkspace = useCallback(
    (workspace: Workspace) => {
      store.setWorkspace(workspace);
    },
    [store],
  );

  const fetchWorkspaces = useCallback(() => {
    store.fetchWorkspaces();
  }, [store]);

  const fetchProjects = useCallback(
    (workspaceId: string, page: number = 0) => {
      store.fetchProjects(workspaceId, page);
    },
    [store],
  );

  const createWorkspace = useCallback(
    async (data: WorkspaceRequest) => {
      return store.createWorkspace(data);
    },
    [store],
  );

  const createProject = useCallback(
    async (workspaceId: string, data: ProjectRequest) => {
      return store.createProject(workspaceId, data);
    },
    [store],
  );

  const deleteProject = useCallback(
    async (workspaceId: string, projectId: string) => {
      return store.deleteProject(workspaceId, projectId);
    },
    [store],
  );

  useEffect(() => {
    fetchWorkspaces();
  }, [fetchWorkspaces]);

  useEffect(() => {
    if (!currentWorkspace) return;
    fetchProjects(currentWorkspace.id, 0);
  }, [currentWorkspace, fetchProjects]);

  return {
    workspaces,
    currentWorkspace,
    projects,
    totalPages,
    isLoading,
    setWorkspace,
    fetchProjects,
    createWorkspace,
    createProject,
    deleteProject,
  };
};
