import { useEffect, useCallback } from 'react';
import { ProjectStore } from '@/store';
import type { ProjectRequest } from '@/lib/api';
import { useWorkspaceStore } from './useWorkspaceStore';

export const useProjectStore = () => {
  const store = ProjectStore.getInstance();
  const { currentWorkspace } = useWorkspaceStore();

  const projects = store.projects?.content ?? [];
  const totalPages = store.projects?.totalPages ?? 1;
  const isLoading = store.isLoading('fetchProjects');

  const fetchProjects = useCallback(
    (workspaceId: string, page: number = 0) => {
      store.fetchProjects(workspaceId, page);
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
    if (!currentWorkspace) return;
    fetchProjects(currentWorkspace.id, 0);
  }, [currentWorkspace, fetchProjects]);

  return {
    projects,
    totalPages,
    isLoading,
    fetchProjects,
    createProject,
    deleteProject,
  };
};
