import { useQuery } from '@tanstack/react-query';
import { getMySharedProjects } from '../api';
import { projectKeys } from './query-keys';
import { useLeaveMySharedProjectMutation } from './useMySharedProjectMutations';

export const useMySharedProjects = (page = 0, size = 5) => {
  const projectsQuery = useQuery({
    queryKey: projectKeys.mySharedList(page, size),
    queryFn: () => getMySharedProjects(page, size),
  });
  const leaveProjectMutation = useLeaveMySharedProjectMutation();

  const leaveProject = (
    projectId: string,
    options?: Parameters<typeof leaveProjectMutation.mutate>[1],
  ) => {
    leaveProjectMutation.mutate(projectId, options);
  };

  return {
    projects: projectsQuery.data?.content ?? [],
    projectsData: projectsQuery.data,
    isPendingProjects: projectsQuery.isPending,
    isProjectsError: projectsQuery.isError,
    refetchProjects: projectsQuery.refetch,
    leaveProject,
  };
};
