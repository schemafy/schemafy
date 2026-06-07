import { useMutation, useQueryClient } from '@tanstack/react-query';
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
  leaveWorkspace,
  rejectInvitation,
  removeMember,
  updateMemberRole,
  updateWorkspace,
} from '../api';
import { workspaceKeys } from './query-keys';

export const useCreateWorkspaceMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateWorkspaceRequest) => createWorkspace(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workspaceKeys.lists() });
    },
  });
};

export const useUpdateWorkspaceMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateWorkspaceRequest }) =>
      updateWorkspace(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: workspaceKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: workspaceKeys.lists() });
    },
  });
};

export const useDeleteWorkspaceMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => deleteWorkspace(id),
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: workspaceKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: workspaceKeys.lists() });
    },
  });
};

export const useLeaveWorkspaceMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (workspaceId: string) => leaveWorkspace(workspaceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workspaceKeys.lists() });
    },
  });
};

export const useRemoveMemberMutation = (workspaceId: string) => {
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

export const useUpdateMemberRoleMutation = (workspaceId: string) => {
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

export const useCreateInvitationMutation = (workspaceId: string) => {
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

export const useAcceptWorkspaceInvitationMutation = () => {
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

export const useRejectWorkspaceInvitationMutation = () => {
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
