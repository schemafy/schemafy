import { useQuery } from '@tanstack/react-query';
import { getProject } from '../api';
import { projectKeys } from './query-keys';

export const useProject = (projectId: string) => {
  const projectQuery = useQuery({
    queryKey: projectKeys.detail(projectId),
    queryFn: () => getProject(projectId),
    enabled: !!projectId,
  });

  return {
    project: projectQuery.data,
    isPendingProject: projectQuery.isPending,
    isLoadingProject: projectQuery.isLoading,
    isProjectError: projectQuery.isError,
    projectError: projectQuery.error,
    refetchProject: projectQuery.refetch,
  };
};
