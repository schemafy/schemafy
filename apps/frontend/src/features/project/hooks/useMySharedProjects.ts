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

  const leaveProject = async (projectId: string) => {
    await leaveProjectMutation.mutateAsync(projectId);
    return projectsQuery.refetch();
  };

  return {
    projects: projectsQuery.data?.content ?? [],
    projectsData: projectsQuery.data,
    leaveProject,
  };
};
