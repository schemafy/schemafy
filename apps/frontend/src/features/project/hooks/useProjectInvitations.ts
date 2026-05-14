import { useQuery } from '@tanstack/react-query';
import type { CreateProjectInvitationRequest } from '../api';
import { getInvitations } from '../api';
import { projectKeys } from './query-keys';
import { useCreateInvitationMutation } from './useProjectMutations';

export const useProjectInvitations = (
  projectId: string,
  page = 0,
  size = 10,
) => {
  const invitationsQuery = useQuery({
    queryKey: projectKeys.invitations(projectId, page, size),
    queryFn: () => getInvitations(projectId, page, size),
    enabled: !!projectId,
  });
  const createInvitationMutation = useCreateInvitationMutation(projectId);

  const createInvitation = (
    data: CreateProjectInvitationRequest,
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
