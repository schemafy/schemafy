import { apiClient } from '../client';
import type { ApiResponse } from '../types';
import type {
  Project,
  ProjectMember,
  ProjectRequest,
  JoinProjectByShareLinkRequest,
  UpdateProjectMemberRoleRequest,
  ProjectsResponse,
  ProjectsMembersResponse,
} from './types';

export const createProject = async (
  workspaceId: string,
  data: ProjectRequest,
): Promise<ApiResponse<Project>> => {
  const response = await apiClient.post<ApiResponse<Project>>(
    `/api/v1.0/workspaces/${workspaceId}/projects`,
    data,
  );
  return response.data;
};

export const getProjects = async (
  workspaceId: string,
  page: number = 0,
  size: number = 5,
): Promise<ApiResponse<ProjectsResponse>> => {
  const response = await apiClient.get<ApiResponse<ProjectsResponse>>(
    `/api/v1.0/workspaces/${workspaceId}/projects`,
    { params: { page, size } },
  );
  return response.data;
};

export const getProject = async (
  workspaceId: string,
  projectId: string,
): Promise<ApiResponse<Project>> => {
  const response = await apiClient.get<ApiResponse<Project>>(
    `/api/v1.0/workspaces/${workspaceId}/projects/${projectId}`,
  );
  return response.data;
};

export const updateProject = async (
  workspaceId: string,
  projectId: string,
  data: ProjectRequest,
): Promise<ApiResponse<Project>> => {
  const response = await apiClient.put<ApiResponse<Project>>(
    `/api/v1.0/workspaces/${workspaceId}/projects/${projectId}`,
    data,
  );
  return response.data;
};

export const deleteProject = async (
  workspaceId: string,
  projectId: string,
): Promise<void> => {
  await apiClient.delete(`/api/v1.0/workspaces/${workspaceId}/projects/${projectId}`);
};

export const getProjectMembers = async (
  workspaceId: string,
  projectId: string,
  page: number = 0,
  size: number = 5,
): Promise<ApiResponse<ProjectsMembersResponse>> => {
  const response = await apiClient.get<ApiResponse<ProjectsMembersResponse>>(
    `/api/v1.0/workspaces/${workspaceId}/projects/${projectId}/members`,
    { params: { page, size } },
  );
  return response.data;
};

export const joinProjectByShareLink = async (
  data: JoinProjectByShareLinkRequest,
): Promise<ApiResponse<ProjectMember>> => {
  const response = await apiClient.post<ApiResponse<ProjectMember>>(
    '/api/v1.0/projects/join',
    data,
  );
  return response.data;
};

export const updateProjectMemberRole = async (
  workspaceId: string,
  projectId: string,
  memberId: string,
  data: UpdateProjectMemberRoleRequest,
): Promise<ApiResponse<ProjectMember>> => {
  const response = await apiClient.patch<ApiResponse<ProjectMember>>(
    `/api/v1.0/workspaces/${workspaceId}/projects/${projectId}/members/${memberId}/role`,
    data,
  );
  return response.data;
};

export const removeProjectMember = async (
  workspaceId: string,
  projectId: string,
  memberId: string,
): Promise<void> => {
  await apiClient.delete(
    `/api/v1.0/workspaces/${workspaceId}/projects/${projectId}/members/${memberId}`,
  );
};

export const leaveProject = async (
  workspaceId: string,
  projectId: string,
): Promise<void> => {
  await apiClient.delete(
    `/api/v1.0/workspaces/${workspaceId}/projects/${projectId}/members/me`,
  );
};
