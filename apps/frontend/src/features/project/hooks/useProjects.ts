import { useQuery } from '@tanstack/react-query';
import type { CreateProjectRequest, UpdateProjectRequest } from '../api';
import { getProjects } from '../api';
import { projectKeys } from './query-keys';
import {
  useCreateProjectMutation,
  useDeleteProjectMutation,
  useLeaveProjectMutation,
  useUpdateProjectMutation,
} from './useProjectMutations';

export const useProjects = (workspaceId: string, page = 0, size = 5) => {
  const projectsQuery = useQuery({
    queryKey: projectKeys.list(workspaceId, page, size),
    queryFn: () => getProjects(workspaceId, page, size),
    enabled: !!workspaceId,
  });
  const createProjectMutation = useCreateProjectMutation(workspaceId);
  const updateProjectMutation = useUpdateProjectMutation(workspaceId);
  const deleteProjectMutation = useDeleteProjectMutation(workspaceId);
  const leaveProjectMutation = useLeaveProjectMutation();

  const createProject = (
    data: CreateProjectRequest,
    options?: Parameters<typeof createProjectMutation.mutate>[1],
  ) => {
    createProjectMutation.mutate(data, options);
  };

  const updateProject = (
    projectId: string,
    data: UpdateProjectRequest,
    options?: Parameters<typeof updateProjectMutation.mutate>[1],
  ) => {
    updateProjectMutation.mutate({ projectId, data }, options);
  };

  const leaveProject = async (projectId: string) => {
    await leaveProjectMutation.mutateAsync(projectId);
    return projectsQuery.refetch();
  };

  const deleteProject = async (projectId: string) => {
    await deleteProjectMutation.mutateAsync(projectId);
    return projectsQuery.refetch();
  };

  return {
    projects: projectsQuery.data?.content ?? [],
    projectsData: projectsQuery.data,
    isLoadingProjects: projectsQuery.isLoading,
    createProject,
    updateProject,
    deleteProject,
    leaveProject,
    isCreatingProject: createProjectMutation.isPending,
    isUpdatingProject: updateProjectMutation.isPending,
    isDeletingProject: deleteProjectMutation.isPending,
    isLeavingProject: leaveProjectMutation.isPending,
  };
};
