import { apiClient } from '../client';
import type { ApiResponse } from '../types';
import type {
  Workspace,
  WorkspaceRequest,
  WorkspacesResponse,
  WorkspaceMemberResponse,
} from './types';

export const createWorkspace = async (
  data?: WorkspaceRequest,
): Promise<ApiResponse<Workspace>> => {
  const response = await apiClient.post<ApiResponse<Workspace>>(
    '/api/v1.0/workspaces',
    data,
  );
  return response.data;
};

export const getWorkspaces = async (
  page: number = 0,
  size: number = 5,
): Promise<ApiResponse<WorkspacesResponse>> => {
  const response = await apiClient.get<ApiResponse<WorkspacesResponse>>(
    '/api/v1.0/workspaces',
    { params: { page, size } },
  );
  return response.data;
};

export const getWorkspace = async (
  workspaceId: string,
): Promise<ApiResponse<Workspace>> => {
  const response = await apiClient.get<ApiResponse<Workspace>>(
    `/api/v1.0/workspaces/${workspaceId}`,
  );
  return response.data;
};

export const updateWorkspace = async (
  workspaceId: string,
  data: WorkspaceRequest,
): Promise<ApiResponse<Workspace>> => {
  const response = await apiClient.put<ApiResponse<Workspace>>(
    `/api/v1.0/workspaces/${workspaceId}`,
    data,
  );
  return response.data;
};

export const deleteWorkspace = async (
  workspaceId: string,
): Promise<void> => {
  await apiClient.delete(`/api/v1.0/workspaces/${workspaceId}`);
};

export const getWorkspaceMembers = async (
  workspaceId: string,
  page: number = 0,
  size: number = 5,
): Promise<ApiResponse<WorkspaceMemberResponse>> => {
  const response = await apiClient.get<ApiResponse<WorkspaceMemberResponse>>(
    `/api/v1.0/workspaces/${workspaceId}/members`,
    { params: { page, size } },
  );
  return response.data;
};
