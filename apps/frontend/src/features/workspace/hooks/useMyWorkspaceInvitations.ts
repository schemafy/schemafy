import { useQuery } from '@tanstack/react-query';
import { getMyInvitations } from '../api';
import { workspaceKeys } from './query-keys';
import {
  useAcceptWorkspaceInvitationMutation,
  useRejectWorkspaceInvitationMutation,
} from './useWorkspaceMutations';

export const useMyWorkspaceInvitations = (page = 0, size = 10) => {
  const myInvitationsQuery = useQuery({
    queryKey: workspaceKeys.myInvitations(page, size),
    queryFn: () => getMyInvitations(page, size),
  });
  const acceptWorkspaceInvitationMutation =
    useAcceptWorkspaceInvitationMutation();
  const rejectWorkspaceInvitationMutation =
    useRejectWorkspaceInvitationMutation();

  const acceptWorkspaceInvitation = (
    invitationId: string,
    options?: Parameters<typeof acceptWorkspaceInvitationMutation.mutate>[1],
  ) => {
    acceptWorkspaceInvitationMutation.mutate(invitationId, options);
  };

  const rejectWorkspaceInvitation = (
    invitationId: string,
    options?: Parameters<typeof rejectWorkspaceInvitationMutation.mutate>[1],
  ) => {
    rejectWorkspaceInvitationMutation.mutate(invitationId, options);
  };

  return {
    myWorkspaceInvitations: myInvitationsQuery.data?.content ?? [],
    myWorkspaceInvitationsData: myInvitationsQuery.data,
    acceptWorkspaceInvitation,
    rejectWorkspaceInvitation,
    isAcceptingWorkspaceInvitation: acceptWorkspaceInvitationMutation.isPending,
    isRejectingWorkspaceInvitation: rejectWorkspaceInvitationMutation.isPending,
  };
};
