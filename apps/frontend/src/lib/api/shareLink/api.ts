import { apiClient } from '../client';
import type { ApiResponse } from '../types';
import type { WorkspacesResponse } from '../workspace/types';
import type { CreateShareLinkResponse, CreateShareLinkRequest, GetShareLinkResponse } from './types';

export const createShareLink = async (
  workspaceId: string,
  projectId: string,
  data: CreateShareLinkRequest,
): Promise<ApiResponse<CreateShareLinkResponse>> => {
  const response = await apiClient.post<ApiResponse<CreateShareLinkResponse>>(
    `/api/v1.0/workspaces/${workspaceId}/projects/${projectId}/share-links`,
    data,
  );
  return response.data;
};

export const getShareLinks = async (
  workspaceId: string,
  projectId: string,
  page: number = 0,
  size: number = 20,
): Promise<ApiResponse<WorkspacesResponse<GetShareLinkResponse>>> => {
  const response = await apiClient.get<ApiResponse<WorkspacesResponse<GetShareLinkResponse>>>(
    `/api/v1.0/workspaces/${workspaceId}/projects/${projectId}/share-links`,
    { params: { page, size } },
  );
  return response.data;
};

export const getShareLink = async (
  workspaceId: string,
  projectId: string,
  shareLinkId: string,
): Promise<ApiResponse<CreateShareLinkResponse>> => {
  const response = await apiClient.get<ApiResponse<CreateShareLinkResponse>>(
    `/api/v1.0/workspaces/${workspaceId}/projects/${projectId}/share-links/${shareLinkId}`,
  );
  return response.data;
};

export const revokeShareLink = async (
  workspaceId: string,
  projectId: string,
  shareLinkId: string,
): Promise<ApiResponse<void>> => {
  const response = await apiClient.patch<ApiResponse<void>>(
    `/api/v1.0/workspaces/${workspaceId}/projects/${projectId}/share-links/${shareLinkId}/revoke`,
  );
  return response.data;
};

export const deleteShareLink = async (
  workspaceId: string,
  projectId: string,
  shareLinkId: string,
): Promise<void> => {
  await apiClient.delete(
    `/api/v1.0/workspaces/${workspaceId}/projects/${projectId}/share-links/${shareLinkId}`,
  );
};
