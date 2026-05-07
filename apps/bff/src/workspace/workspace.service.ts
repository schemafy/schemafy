import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';
import type {
  CreateWorkspaceInvitationRequest,
  CreateWorkspaceRequest,
  PageResponse,
  UpdateMemberRoleRequest,
  UpdateWorkspaceRequest,
  WorkspaceInvitationCreateResponse,
  WorkspaceInvitationResponse,
  WorkspaceMemberResponse,
  WorkspaceResponse,
  WorkspaceSummaryResponse,
} from './workspace.types';

@Injectable()
export class WorkspaceService {
  constructor(private readonly backendClient: BackendClientService) {}

  async createWorkspace(
    data: CreateWorkspaceRequest,
    authHeader: string,
  ): Promise<WorkspaceResponse> {
    const response = await this.backendClient.client.post<WorkspaceResponse>(
      '/api/v1.0/workspaces',
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getWorkspaces(
    page: number,
    size: number,
    authHeader: string,
  ): Promise<PageResponse<WorkspaceSummaryResponse>> {
    const response = await this.backendClient.client.get<
      PageResponse<WorkspaceSummaryResponse>
    >('/api/v1.0/workspaces', {
      ...this.backendClient.getAuthConfig(authHeader),
      params: { page, size },
    });
    return response.data;
  }

  async getWorkspace(
    id: string,
    authHeader: string,
  ): Promise<WorkspaceResponse> {
    const response = await this.backendClient.client.get<WorkspaceResponse>(
      `/api/v1.0/workspaces/${id}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async updateWorkspace(
    id: string,
    data: UpdateWorkspaceRequest,
    authHeader: string,
  ): Promise<WorkspaceResponse> {
    const response = await this.backendClient.client.put<WorkspaceResponse>(
      `/api/v1.0/workspaces/${id}`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async deleteWorkspace(id: string, authHeader: string): Promise<null> {
    const response = await this.backendClient.client.delete<null>(
      `/api/v1.0/workspaces/${id}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getMembers(
    id: string,
    page: number,
    size: number,
    authHeader: string,
  ): Promise<PageResponse<WorkspaceMemberResponse>> {
    const response = await this.backendClient.client.get<
      PageResponse<WorkspaceMemberResponse>
    >(`/api/v1.0/workspaces/${id}/members`, {
      ...this.backendClient.getAuthConfig(authHeader),
      params: { page, size },
    });
    return response.data;
  }

  async removeMember(
    workspaceId: string,
    userId: string,
    authHeader: string,
  ): Promise<null> {
    const response = await this.backendClient.client.delete<null>(
      `/api/v1.0/workspaces/${workspaceId}/members/${userId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async leaveWorkspace(workspaceId: string, authHeader: string): Promise<null> {
    const response = await this.backendClient.client.delete<null>(
      `/api/v1.0/workspaces/${workspaceId}/members/me`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async updateMemberRole(
    workspaceId: string,
    userId: string,
    data: UpdateMemberRoleRequest,
    authHeader: string,
  ): Promise<WorkspaceMemberResponse> {
    const response =
      await this.backendClient.client.patch<WorkspaceMemberResponse>(
        `/api/v1.0/workspaces/${workspaceId}/members/${userId}/role`,
        data,
        this.backendClient.getAuthConfig(authHeader),
      );
    return response.data;
  }

  async createInvitation(
    workspaceId: string,
    data: CreateWorkspaceInvitationRequest,
    authHeader: string,
  ): Promise<WorkspaceInvitationCreateResponse> {
    const response =
      await this.backendClient.client.post<WorkspaceInvitationCreateResponse>(
        `/api/v1.0/workspaces/${workspaceId}/invitations`,
        data,
        this.backendClient.getAuthConfig(authHeader),
      );
    return response.data;
  }

  async getInvitations(
    workspaceId: string,
    page: number,
    size: number,
    authHeader: string,
  ): Promise<PageResponse<WorkspaceInvitationResponse>> {
    const response = await this.backendClient.client.get<
      PageResponse<WorkspaceInvitationResponse>
    >(`/api/v1.0/workspaces/${workspaceId}/invitations`, {
      ...this.backendClient.getAuthConfig(authHeader),
      params: { page, size },
    });
    return response.data;
  }

  async getMyInvitations(
    page: number,
    size: number,
    authHeader: string,
  ): Promise<PageResponse<WorkspaceInvitationResponse>> {
    const response = await this.backendClient.client.get<
      PageResponse<WorkspaceInvitationResponse>
    >('/api/v1.0/users/me/invitations/workspaces', {
      ...this.backendClient.getAuthConfig(authHeader),
      params: { page, size },
    });
    return response.data;
  }

  async acceptInvitation(
    invitationId: string,
    authHeader: string,
  ): Promise<WorkspaceMemberResponse> {
    const response =
      await this.backendClient.client.patch<WorkspaceMemberResponse>(
        `/api/v1.0/workspaces/invitations/${invitationId}/accept`,
        {},
        this.backendClient.getAuthConfig(authHeader),
      );
    return response.data;
  }

  async rejectInvitation(
    invitationId: string,
    authHeader: string,
  ): Promise<null> {
    const response = await this.backendClient.client.patch<null>(
      `/api/v1.0/workspaces/invitations/${invitationId}/reject`,
      {},
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }
}
