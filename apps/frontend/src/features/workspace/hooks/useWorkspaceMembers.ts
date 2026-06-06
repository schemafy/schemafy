import { useQuery } from '@tanstack/react-query';
import type { UpdateMemberRoleRequest } from '../api';
import { getMembers } from '../api';
import { workspaceKeys } from './query-keys';
import {
  useRemoveMemberMutation,
  useUpdateMemberRoleMutation,
} from './useWorkspaceMutations';

export const useWorkspaceMembers = (
  workspaceId: string,
  page = 0,
  size = 5,
) => {
  const membersQuery = useQuery({
    queryKey: workspaceKeys.members(workspaceId, page, size),
    queryFn: () => getMembers(workspaceId, page, size),
    enabled: !!workspaceId,
  });
  const removeMemberMutation = useRemoveMemberMutation(workspaceId);
  const updateMemberRoleMutation = useUpdateMemberRoleMutation(workspaceId);

  const removeMember = (
    userId: string,
    options?: Parameters<typeof removeMemberMutation.mutate>[1],
  ) => {
    removeMemberMutation.mutate(userId, options);
  };

  const updateMemberRole = (
    userId: string,
    data: UpdateMemberRoleRequest,
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
