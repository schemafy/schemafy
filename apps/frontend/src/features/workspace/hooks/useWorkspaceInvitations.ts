import { useQuery } from '@tanstack/react-query';
import type { CreateWorkspaceInvitationRequest } from '../api';
import { getInvitations } from '../api';
import { workspaceKeys } from './query-keys';
import { useCreateInvitationMutation } from './useWorkspaceMutations';

type UseWorkspaceInvitationsOptions = {
  page?: number;
  size?: number;
  enabled?: boolean;
};

export const useWorkspaceInvitations = (
  workspaceId: string,
  options: UseWorkspaceInvitationsOptions = {},
) => {
  const { page = 0, size = 10, enabled = true } = options;

  const invitationsQuery = useQuery({
    queryKey: workspaceKeys.invitations(workspaceId, page, size),
    queryFn: () => getInvitations(workspaceId, page, size),
    enabled: !!workspaceId && enabled,
  });
  const createInvitationMutation = useCreateInvitationMutation(workspaceId);

  const createInvitation = (
    data: CreateWorkspaceInvitationRequest,
    options?: Parameters<typeof createInvitationMutation.mutate>[1],
  ) => {
    createInvitationMutation.mutate(data, options);
  };

  return {
    invitations: invitationsQuery.data?.content ?? [],
    invitationsData: invitationsQuery.data,
    isLoadingInvitations: invitationsQuery.isLoading,
    createInvitation,
    isCreatingInvitation: createInvitationMutation.isPending,
  };
};
