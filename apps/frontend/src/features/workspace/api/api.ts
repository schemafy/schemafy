import { apiClient, type PageResponse } from '@/lib/api';
import type {
  CreateWorkspaceInvitationRequest,
  CreateWorkspaceRequest,
  UpdateMemberRoleRequest,
  UpdateWorkspaceRequest,
  WorkspaceInvitationCreateResponse,
  WorkspaceInvitationResponse,
  WorkspaceMemberResponse,
  WorkspaceResponse,
  WorkspaceSummaryResponse,
} from './types';

export const createWorkspace = async (
  data: CreateWorkspaceRequest,
): Promise<WorkspaceResponse> => {
  const response = await apiClient.post<WorkspaceResponse>('/workspaces', data);
  return response.data;
};

export const getWorkspaces = async (
  page = 0,
  size = 5,
): Promise<PageResponse<WorkspaceSummaryResponse>> => {
  const response = await apiClient.get<PageResponse<WorkspaceSummaryResponse>>(
    '/workspaces',
    {params: {page, size}},
  );
  return response.data;
};

export const getWorkspace = async (id: string): Promise<WorkspaceResponse> => {
  const response = await apiClient.get<WorkspaceResponse>(`/workspaces/${id}`);
  return response.data;
};

export const updateWorkspace = async (
  id: string,
  data: UpdateWorkspaceRequest,
): Promise<WorkspaceResponse> => {
  const response = await apiClient.put<WorkspaceResponse>(
    `/workspaces/${id}`,
    data,
  );
  return response.data;
};

export const deleteWorkspace = async (id: string): Promise<null> => {
  const response = await apiClient.delete<null>(`/workspaces/${id}`);
  return response.data;
};

export const getMembers = async (
  workspaceId: string,
  page = 0,
  size = 5,
): Promise<PageResponse<WorkspaceMemberResponse>> => {
  const response = await apiClient.get<PageResponse<WorkspaceMemberResponse>>(
    `/workspaces/${workspaceId}/members`,
    {params: {page, size}},
  );
  return response.data;
};

export const leaveWorkspace = async (workspaceId: string): Promise<null> => {
  const response = await apiClient.delete<null>(
    `/workspaces/${workspaceId}/members/me`,
  );
  return response.data;
};

export const removeMember = async (
  workspaceId: string,
  userId: string,
): Promise<null> => {
  const response = await apiClient.delete<null>(
    `/workspaces/${workspaceId}/members/${userId}`,
  );
  return response.data;
};

export const updateMemberRole = async (
  workspaceId: string,
  userId: string,
  data: UpdateMemberRoleRequest,
): Promise<WorkspaceMemberResponse> => {
  const response = await apiClient.patch<WorkspaceMemberResponse>(
    `/workspaces/${workspaceId}/members/${userId}/role`,
    data,
  );
  return response.data;
};

export const createInvitation = async (
  workspaceId: string,
  data: CreateWorkspaceInvitationRequest,
): Promise<WorkspaceInvitationCreateResponse> => {
  const response = await apiClient.post<WorkspaceInvitationCreateResponse>(
    `/workspaces/${workspaceId}/invitations`,
    data,
  );
  return response.data;
};

export const getInvitations = async (
  workspaceId: string,
  page = 0,
  size = 10,
): Promise<PageResponse<WorkspaceInvitationResponse>> => {
  const response = await apiClient.get<
    PageResponse<WorkspaceInvitationResponse>
  >(`/workspaces/${workspaceId}/invitations`, {params: {page, size}});
  return response.data;
};

export const getMyInvitations = async (
  page = 0,
  size = 10,
): Promise<PageResponse<WorkspaceInvitationResponse>> => {
  const response = await apiClient.get<
    PageResponse<WorkspaceInvitationResponse>
  >('/users/me/invitations/workspaces', {params: {page, size}});
  return response.data;
};

export const acceptInvitation = async (
  invitationId: string,
): Promise<WorkspaceMemberResponse> => {
  const response = await apiClient.patch<WorkspaceMemberResponse>(
    `/workspaces/invitations/${invitationId}/accept`,
  );
  return response.data;
};

export const rejectInvitation = async (invitationId: string): Promise<null> => {
  const response = await apiClient.patch<null>(
    `/workspaces/invitations/${invitationId}/reject`,
  );
  return response.data;
};
