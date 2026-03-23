import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type {
  CreateWorkspaceInvitationRequest,
  CreateWorkspaceRequest,
  UpdateMemberRoleRequest,
  UpdateWorkspaceRequest,
} from '../api';
import {
  acceptInvitation,
  createInvitation,
  createWorkspace,
  deleteWorkspace,
  getInvitations,
  getMembers,
  getMyInvitations,
  getWorkspace,
  getWorkspaces,
  leaveWorkspace,
  rejectInvitation,
  removeMember,
  updateMemberRole,
  updateWorkspace,
} from '../api';
import { workspaceKeys } from './query-keys';

export const useGetWorkspaces = (page = 0, size = 5) => {
  return useQuery({
    queryKey: workspaceKeys.list(page, size),
    queryFn: () => getWorkspaces(page, size),
  });
};

export const useGetWorkspace = (id: string) => {
  return useQuery({
    queryKey: workspaceKeys.detail(id),
    queryFn: () => getWorkspace(id),
    enabled: !!id,
  });
};

export const useCreateWorkspace = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateWorkspaceRequest) => createWorkspace(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workspaceKeys.lists() });
    },
  });
};

export const useUpdateWorkspace = (id: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateWorkspaceRequest) => updateWorkspace(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workspaceKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: workspaceKeys.lists() });
    },
  });
};

export const useDeleteWorkspace = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => deleteWorkspace(id),
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: workspaceKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: workspaceKeys.lists() });
    },
  });
};

export const useGetMembers = (workspaceId: string, page = 0, size = 5) => {
  return useQuery({
    queryKey: workspaceKeys.members(workspaceId, page, size),
    queryFn: () => getMembers(workspaceId, page, size),
    enabled: !!workspaceId,
  });
};

export const useLeaveWorkspace = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (workspaceId: string) => leaveWorkspace(workspaceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workspaceKeys.lists() });
    },
  });
};

export const useRemoveMember = (workspaceId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (userId: string) => removeMember(workspaceId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: workspaceKeys.membersAll(workspaceId),
      });
    },
  });
};

export const useUpdateMemberRole = (workspaceId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      userId,
      data,
    }: {
      userId: string;
      data: UpdateMemberRoleRequest;
    }) => updateMemberRole(workspaceId, userId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: workspaceKeys.membersAll(workspaceId),
      });
    },
  });
};

export const useGetInvitations = (workspaceId: string, page = 0, size = 10) => {
  return useQuery({
    queryKey: workspaceKeys.invitations(workspaceId, page, size),
    queryFn: () => getInvitations(workspaceId, page, size),
    enabled: !!workspaceId,
  });
};

export const useCreateInvitation = (workspaceId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateWorkspaceInvitationRequest) =>
      createInvitation(workspaceId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: workspaceKeys.invitationsAll(workspaceId),
      });
    },
  });
};

export const useGetMyWorkspaceInvitations = (page = 0, size = 10) => {
  return useQuery({
    queryKey: workspaceKeys.myInvitations(page, size),
    queryFn: () => getMyInvitations(page, size),
  });
};

export const useAcceptWorkspaceInvitation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: string) => acceptInvitation(invitationId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: workspaceKeys.myInvitationsAll(),
      });
      queryClient.invalidateQueries({ queryKey: workspaceKeys.lists() });
    },
  });
};

export const useRejectWorkspaceInvitation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: string) => rejectInvitation(invitationId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: workspaceKeys.myInvitationsAll(),
      });
    },
  });
};
