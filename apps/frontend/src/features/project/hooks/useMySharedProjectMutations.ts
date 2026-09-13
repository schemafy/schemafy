import { useMutation, useQueryClient } from '@tanstack/react-query';
import { leaveProject } from '../api';
import { projectKeys } from './query-keys';

export const useLeaveMySharedProjectMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (projectId: string) => leaveProject(projectId),
    onSuccess: (_, projectId) => {
      queryClient.removeQueries({ queryKey: projectKeys.detail(projectId) });
      queryClient.invalidateQueries({
        queryKey: projectKeys.mySharedLists(),
      });
    },
  });
};
