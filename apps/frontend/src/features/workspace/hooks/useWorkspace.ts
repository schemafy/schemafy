import { useQuery } from '@tanstack/react-query';
import { getWorkspace } from '../api';
import { workspaceKeys } from './query-keys';

export const useWorkspace = (id: string) => {
  const workspaceQuery = useQuery({
    queryKey: workspaceKeys.detail(id),
    queryFn: () => getWorkspace(id),
    enabled: !!id,
  });

  return {
    workspace: workspaceQuery.data,
    isLoadingWorkspace: workspaceQuery.isLoading,
  };
};
