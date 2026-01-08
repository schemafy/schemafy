import { useEffect, useCallback } from 'react';
import { ProjectStore, WorkspaceStore } from '@/store';
import type { ProjectRequest } from '@/lib/api';

export const useProjectStore = () => {
  const store = ProjectStore.getInstance();
  const workspaceStore = WorkspaceStore.getInstance();
  const currentWorkspace = workspaceStore.currentWorkspace;

  const projects = store.projects?.content ?? [];
  const totalPages = store.projects?.totalPages ?? 1;
  const isLoading = store.isLoading('fetchProjects');

  const fetchProjects = useCallback(
    (workspaceId: string, page: number = 0) => {
      store.fetchProjects(workspaceId, page);
    },
    [store],
  );

  const createProject = async (workspaceId: string, data: ProjectRequest) => {
    store.createProject(workspaceId, data);
  };

  const updateProject = async (
    workspaceId: string,
    projectId: string,
    data: ProjectRequest,
  ) => {
    store.updateProject(workspaceId, projectId, data);
  };

  const deleteProject = async (workspaceId: string, projectId: string) => {
    return store.deleteProject(workspaceId, projectId);
  };

  useEffect(() => {
    if (!currentWorkspace) return;
    fetchProjects(currentWorkspace.id, 0);
  }, [currentWorkspace, fetchProjects]);

  return {
    projects,
    totalPages,
    isLoading,
    fetchProjects,
    createProject,
    updateProject,
    deleteProject,
  };
};
