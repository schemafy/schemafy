import { useMutation, useQueryClient } from '@tanstack/react-query';
import type {
  CreateProjectInvitationRequest,
  CreateProjectRequest,
  UpdateProjectMemberRoleRequest,
  UpdateProjectRequest,
} from '../api';
import {
  acceptInvitation,
  createInvitation,
  createProject,
  deleteProject,
  leaveProject,
  rejectInvitation,
  removeMember,
  updateMemberRole,
  updateProject,
} from '../api';
import { projectKeys } from './query-keys';

export const useCreateProjectMutation = (workspaceId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateProjectRequest) =>
      createProject(workspaceId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: projectKeys.lists(workspaceId),
      });
    },
  });
};

export const useUpdateProjectMutation = (workspaceId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      projectId,
      data,
    }: {
      projectId: string;
      data: UpdateProjectRequest;
    }) => updateProject(projectId, data),
    onSuccess: (_, { projectId }) => {
      queryClient.invalidateQueries({
        queryKey: projectKeys.detail(projectId),
      });
      queryClient.invalidateQueries({
        queryKey: projectKeys.lists(workspaceId),
      });
    },
  });
};

export const useDeleteProjectMutation = (workspaceId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (projectId: string) => deleteProject(projectId),
    onSuccess: (_, projectId) => {
      queryClient.removeQueries({ queryKey: projectKeys.detail(projectId) });
      queryClient.invalidateQueries({
        queryKey: projectKeys.lists(workspaceId),
      });
    },
  });
};

export const useLeaveProjectMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (projectId: string) => leaveProject(projectId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.all });
    },
  });
};

export const useRemoveMemberMutation = (projectId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (userId: string) => removeMember(projectId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: projectKeys.membersAll(projectId),
      });
    },
  });
};

export const useUpdateMemberRoleMutation = (projectId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      userId,
      data,
    }: {
      userId: string;
      data: UpdateProjectMemberRoleRequest;
    }) => updateMemberRole(projectId, userId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: projectKeys.membersAll(projectId),
      });
    },
  });
};

export const useCreateInvitationMutation = (projectId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateProjectInvitationRequest) =>
      createInvitation(projectId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: projectKeys.invitationsAll(projectId),
      });
    },
  });
};

export const useAcceptProjectInvitationMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: string) => acceptInvitation(invitationId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: projectKeys.myInvitationsAll(),
      });
      queryClient.invalidateQueries({ queryKey: projectKeys.all });
    },
  });
};

export const useRejectProjectInvitationMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: string) => rejectInvitation(invitationId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: projectKeys.myInvitationsAll(),
      });
    },
  });
};
