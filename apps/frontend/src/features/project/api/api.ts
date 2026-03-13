import { apiClient, type PageResponse } from '@/lib/api';
import type {
  CreateProjectInvitationRequest,
  CreateProjectRequest,
  ProjectInvitationCreateResponse,
  ProjectInvitationResponse,
  ProjectMemberResponse,
  ProjectResponse,
  ProjectSummaryResponse,
  UpdateProjectMemberRoleRequest,
  UpdateProjectRequest
} from './types';

export const createProject = async (
  workspaceId: string,
  data: CreateProjectRequest,
): Promise<ProjectResponse> => {
  const response = await apiClient.post<ProjectResponse>(
    `/workspaces/${workspaceId}/projects`,
    data,
  );
  return response.data;
};

export const getProjects = async (
  workspaceId: string,
  page = 0,
  size = 5,
): Promise<PageResponse<ProjectSummaryResponse>> => {
  const response = await apiClient.get<PageResponse<ProjectSummaryResponse>>(
    `/workspaces/${workspaceId}/projects`,
    {params: {page, size}},
  );
  return response.data;
};

export const getProject = async (
  projectId: string,
): Promise<ProjectResponse> => {
  const response = await apiClient.get<ProjectResponse>(
    `/projects/${projectId}`,
  );
  return response.data;
};

export const updateProject = async (
  projectId: string,
  data: UpdateProjectRequest,
): Promise<ProjectResponse> => {
  const response = await apiClient.put<ProjectResponse>(
    `/projects/${projectId}`,
    data,
  );
  return response.data;
};

export const deleteProject = async (projectId: string): Promise<null> => {
  const response = await apiClient.delete<null>(`/projects/${projectId}`);
  return response.data;
};

export const getMembers = async (
  projectId: string,
  page = 0,
  size = 5,
): Promise<PageResponse<ProjectMemberResponse>> => {
  const response = await apiClient.get<PageResponse<ProjectMemberResponse>>(
    `/projects/${projectId}/members`,
    {params: {page, size}},
  );
  return response.data;
};

export const updateMemberRole = async (
  projectId: string,
  userId: string,
  data: UpdateProjectMemberRoleRequest,
): Promise<ProjectMemberResponse> => {
  const response = await apiClient.patch<ProjectMemberResponse>(
    `/projects/${projectId}/members/${userId}/role`,
    data,
  );
  return response.data;
};

export const leaveProject = async (projectId: string): Promise<null> => {
  const response = await apiClient.delete<null>(
    `/projects/${projectId}/members/me`,
  );
  return response.data;
};

export const removeMember = async (
  projectId: string,
  userId: string,
): Promise<null> => {
  const response = await apiClient.delete<null>(
    `/projects/${projectId}/members/${userId}`,
  );
  return response.data;
};

export const createInvitation = async (
  projectId: string,
  data: CreateProjectInvitationRequest,
): Promise<ProjectInvitationCreateResponse> => {
  const response = await apiClient.post<ProjectInvitationCreateResponse>(
    `/projects/${projectId}/invitations`,
    data,
  );
  return response.data;
};

export const getInvitations = async (
  projectId: string,
  page = 0,
  size = 10,
): Promise<PageResponse<ProjectInvitationResponse>> => {
  const response = await apiClient.get<PageResponse<ProjectInvitationResponse>>(
    `/projects/${projectId}/invitations`,
    {params: {page, size}},
  );
  return response.data;
};

export const getMyInvitations = async (
  page = 0,
  size = 10,
): Promise<PageResponse<ProjectInvitationResponse>> => {
  const response = await apiClient.get<PageResponse<ProjectInvitationResponse>>(
    '/users/me/invitations/projects',
    {params: {page, size}},
  );
  return response.data;
};

export const acceptInvitation = async (
  invitationId: string,
): Promise<ProjectMemberResponse> => {
  const response = await apiClient.patch<ProjectMemberResponse>(
    `/projects/invitations/${invitationId}/accept`,
  );
  return response.data;
};

export const rejectInvitation = async (
  invitationId: string,
): Promise<null> => {
  const response = await apiClient.patch<null>(
    `/projects/invitations/${invitationId}/reject`,
  );
  return response.data;
};
