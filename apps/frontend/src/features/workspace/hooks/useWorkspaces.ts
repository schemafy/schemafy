import { useQuery } from '@tanstack/react-query';
import type { CreateWorkspaceRequest, UpdateWorkspaceRequest } from '../api';
import { getWorkspaces } from '../api';
import { workspaceKeys } from './query-keys';
import {
  useCreateWorkspaceMutation,
  useDeleteWorkspaceMutation,
  useLeaveWorkspaceMutation,
  useUpdateWorkspaceMutation,
} from './useWorkspaceMutations';

export const useWorkspaces = (page = 0, size = 5) => {
  const workspacesQuery = useQuery({
    queryKey: workspaceKeys.list(page, size),
    queryFn: () => getWorkspaces(page, size),
  });
  const createWorkspaceMutation = useCreateWorkspaceMutation();
  const updateWorkspaceMutation = useUpdateWorkspaceMutation();
  const deleteWorkspaceMutation = useDeleteWorkspaceMutation();
  const leaveWorkspaceMutation = useLeaveWorkspaceMutation();

  const createWorkspace = (
    data: CreateWorkspaceRequest,
    options?: Parameters<typeof createWorkspaceMutation.mutate>[1],
  ) => {
    createWorkspaceMutation.mutate(data, options);
  };

  const updateWorkspace = (
    id: string,
    data: UpdateWorkspaceRequest,
    options?: Parameters<typeof updateWorkspaceMutation.mutate>[1],
  ) => {
    updateWorkspaceMutation.mutate({ id, data }, options);
  };

  const deleteWorkspace = (
    id: string,
    options?: Parameters<typeof deleteWorkspaceMutation.mutate>[1],
  ) => {
    deleteWorkspaceMutation.mutate(id, options);
  };

  const leaveWorkspace = (
    workspaceId: string,
    options?: Parameters<typeof leaveWorkspaceMutation.mutate>[1],
  ) => {
    leaveWorkspaceMutation.mutate(workspaceId, options);
  };

  return {
    workspaces: workspacesQuery.data?.content ?? [],
    workspacesData: workspacesQuery.data,
    isPendingWorkspaces: workspacesQuery.isPending,
    isLoadingWorkspaces: workspacesQuery.isLoading,
    isWorkspacesError: workspacesQuery.isError,
    workspacesError: workspacesQuery.error,
    refetchWorkspaces: workspacesQuery.refetch,
    createWorkspace,
    updateWorkspace,
    deleteWorkspace,
    leaveWorkspace,
    isCreatingWorkspace: createWorkspaceMutation.isPending,
    isUpdatingWorkspace: updateWorkspaceMutation.isPending,
    isDeletingWorkspace: deleteWorkspaceMutation.isPending,
    isLeavingWorkspace: leaveWorkspaceMutation.isPending,
  };
};
