import { useQuery } from '@tanstack/react-query';
import type { UpdateProjectMemberRoleRequest } from '../api';
import { getMembers } from '../api';
import { projectKeys } from './query-keys';
import {
  useRemoveMemberMutation,
  useUpdateMemberRoleMutation,
} from './useProjectMutations';

export const useProjectMembers = (projectId: string, page = 0, size = 5) => {
  const membersQuery = useQuery({
    queryKey: projectKeys.members(projectId, page, size),
    queryFn: () => getMembers(projectId, page, size),
    enabled: !!projectId,
  });
  const removeMemberMutation = useRemoveMemberMutation(projectId);
  const updateMemberRoleMutation = useUpdateMemberRoleMutation(projectId);

  const removeMember = (
    userId: string,
    options?: Parameters<typeof removeMemberMutation.mutate>[1],
  ) => {
    removeMemberMutation.mutate(userId, options);
  };

  const updateMemberRole = (
    userId: string,
    data: UpdateProjectMemberRoleRequest,
    options?: Parameters<typeof updateMemberRoleMutation.mutate>[1],
  ) => {
    updateMemberRoleMutation.mutate({ userId, data }, options);
  };

  return {
    members: membersQuery.data?.content ?? [],
    membersData: membersQuery.data,
    isLoadingMembers: membersQuery.isLoading,
    removeMember,
    updateMemberRole,
    isRemovingMember: removeMemberMutation.isPending,
    isUpdatingMemberRole: updateMemberRoleMutation.isPending,
  };
};
