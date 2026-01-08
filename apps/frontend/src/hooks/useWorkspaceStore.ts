import { useEffect, useCallback } from 'react';
import { ProjectStore } from '@/store';
import type { WorkspaceRequest, Workspace } from '@/lib/api';

export const useWorkspaceStore = () => {
  const store = ProjectStore.getInstance();

  const workspaces = store.workspaces?.content ?? [];
  const currentWorkspace = store.currentWorkspace;

  const setWorkspace = (workspace: Workspace) => {
    store.setWorkspace(workspace);
  };

  const fetchWorkspaces = useCallback(() => {
    store.fetchWorkspaces();
  }, [store]);

  const createWorkspace = (data: WorkspaceRequest) => {
    return store.createWorkspace(data);
  };

  const updateWorkspace = (workspaceId: string, data: WorkspaceRequest) => {
    store.updateWorkspace(workspaceId, data);
  };

  const deleteWorkspace = (workspaceId: string) => {
    store.deleteWorkspace(workspaceId);
  };

  useEffect(() => {
    fetchWorkspaces();
  }, [fetchWorkspaces]);

  return {
    workspaces,
    currentWorkspace,
    setWorkspace,
    createWorkspace,
    updateWorkspace,
    deleteWorkspace,
    fetchWorkspaces,
  };
};
