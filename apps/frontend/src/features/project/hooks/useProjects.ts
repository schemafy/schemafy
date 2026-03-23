import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
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
  getInvitations,
  getMembers,
  getMyInvitations,
  getProject,
  getProjects,
  leaveProject,
  rejectInvitation,
  removeMember,
  updateMemberRole,
  updateProject,
} from '../api';
import { projectKeys } from './query-keys';

export const useGetProjects = (workspaceId: string, page = 0, size = 5) => {
  return useQuery({
    queryKey: projectKeys.list(workspaceId, page, size),
    queryFn: () => getProjects(workspaceId, page, size),
    enabled: !!workspaceId,
  });
};

export const useGetProject = (projectId: string) => {
  return useQuery({
    queryKey: projectKeys.detail(projectId),
    queryFn: () => getProject(projectId),
    enabled: !!projectId,
  });
};

export const useCreateProject = (workspaceId: string) => {
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

export const useUpdateProject = (projectId: string, workspaceId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateProjectRequest) => updateProject(projectId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: projectKeys.detail(projectId),
      });
      queryClient.invalidateQueries({
        queryKey: projectKeys.lists(workspaceId),
      });
    },
  });
};

export const useDeleteProject = (workspaceId: string) => {
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

export const useGetMembers = (projectId: string, page = 0, size = 5) => {
  return useQuery({
    queryKey: projectKeys.members(projectId, page, size),
    queryFn: () => getMembers(projectId, page, size),
    enabled: !!projectId,
  });
};

export const useLeaveProject = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (projectId: string) => leaveProject(projectId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.all });
    },
  });
};

export const useRemoveMember = (projectId: string) => {
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

export const useUpdateMemberRole = (projectId: string) => {
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

export const useGetInvitations = (projectId: string, page = 0, size = 10) => {
  return useQuery({
    queryKey: projectKeys.invitations(projectId, page, size),
    queryFn: () => getInvitations(projectId, page, size),
    enabled: !!projectId,
  });
};

export const useCreateInvitation = (projectId: string) => {
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

export const useGetMyProjectInvitations = (page = 0, size = 10) => {
  return useQuery({
    queryKey: projectKeys.myInvitations(page, size),
    queryFn: () => getMyInvitations(page, size),
  });
};

export const useAcceptProjectInvitation = () => {
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

export const useRejectProjectInvitation = () => {
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
