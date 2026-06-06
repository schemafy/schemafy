import { useQuery } from '@tanstack/react-query';
import { getMyInvitations } from '../api';
import { projectKeys } from './query-keys';
import {
  useAcceptProjectInvitationMutation,
  useRejectProjectInvitationMutation,
} from './useProjectMutations';

export const useMyProjectInvitations = (page = 0, size = 10) => {
  const myInvitationsQuery = useQuery({
    queryKey: projectKeys.myInvitations(page, size),
    queryFn: () => getMyInvitations(page, size),
  });
  const acceptProjectInvitationMutation = useAcceptProjectInvitationMutation();
  const rejectProjectInvitationMutation = useRejectProjectInvitationMutation();

  const acceptProjectInvitation = (
    invitationId: string,
    options?: Parameters<typeof acceptProjectInvitationMutation.mutate>[1],
  ) => {
    acceptProjectInvitationMutation.mutate(invitationId, options);
  };

  const rejectProjectInvitation = (
    invitationId: string,
    options?: Parameters<typeof rejectProjectInvitationMutation.mutate>[1],
  ) => {
    rejectProjectInvitationMutation.mutate(invitationId, options);
  };

  return {
    myProjectInvitations: myInvitationsQuery.data?.content ?? [],
    myProjectInvitationsData: myInvitationsQuery.data,
    acceptProjectInvitation,
    rejectProjectInvitation,
    isAcceptingProjectInvitation: acceptProjectInvitationMutation.isPending,
    isRejectingProjectInvitation: rejectProjectInvitationMutation.isPending,
  };
};
