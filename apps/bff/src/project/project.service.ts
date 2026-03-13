import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';
import type {
  CreateProjectInvitationRequest,
  CreateProjectRequest,
  ProjectInvitationCreateResponse,
  ProjectInvitationResponse,
  ProjectMemberResponse,
  ProjectResponse,
  ProjectSummaryResponse,
  UpdateProjectMemberRoleRequest,
  UpdateProjectRequest,
} from './project.types';
import { PageResponse } from "../common/types/api-response.types";

@Injectable()
export class ProjectService {
  constructor(private readonly backendClient: BackendClientService) {
  }

  async createProject(
    workspaceId: string,
    data: CreateProjectRequest,
    authHeader: string,
  ): Promise<ProjectResponse> {
    const response = await this.backendClient.client.post<ProjectResponse>(
      `/api/v1.0/workspaces/${workspaceId}/projects`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getProjects(
    workspaceId: string,
    page: number,
    size: number,
    authHeader: string,
  ): Promise<PageResponse<ProjectSummaryResponse>> {
    const response = await this.backendClient.client.get<
      PageResponse<ProjectSummaryResponse>
    >(`/api/v1.0/workspaces/${workspaceId}/projects`, {
      ...this.backendClient.getAuthConfig(authHeader),
      params: {page, size},
    });
    return response.data;
  }

  async getProject(
    projectId: string,
    authHeader: string,
  ): Promise<ProjectResponse> {
    const response = await this.backendClient.client.get<ProjectResponse>(
      `/api/v1.0/projects/${projectId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async updateProject(
    projectId: string,
    data: UpdateProjectRequest,
    authHeader: string,
  ): Promise<ProjectResponse> {
    const response = await this.backendClient.client.put<ProjectResponse>(
      `/api/v1.0/projects/${projectId}`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async deleteProject(projectId: string, authHeader: string): Promise<null> {
    const response = await this.backendClient.client.delete<null>(
      `/api/v1.0/projects/${projectId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getMembers(
    projectId: string,
    page: number,
    size: number,
    authHeader: string,
  ): Promise<PageResponse<ProjectMemberResponse>> {
    const response = await this.backendClient.client.get<
      PageResponse<ProjectMemberResponse>
    >(`/api/v1.0/projects/${projectId}/members`, {
      ...this.backendClient.getAuthConfig(authHeader),
      params: {page, size},
    });
    return response.data;
  }

  async updateMemberRole(
    projectId: string,
    userId: string,
    data: UpdateProjectMemberRoleRequest,
    authHeader: string,
  ): Promise<ProjectMemberResponse> {
    const response =
      await this.backendClient.client.patch<ProjectMemberResponse>(
        `/api/v1.0/projects/${projectId}/members/${userId}/role`,
        data,
        this.backendClient.getAuthConfig(authHeader),
      );
    return response.data;
  }

  async removeMember(
    projectId: string,
    userId: string,
    authHeader: string,
  ): Promise<null> {
    const response = await this.backendClient.client.delete<null>(
      `/api/v1.0/projects/${projectId}/members/${userId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async leaveProject(projectId: string, authHeader: string): Promise<null> {
    const response = await this.backendClient.client.delete<null>(
      `/api/v1.0/projects/${projectId}/members/me`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async createInvitation(
    projectId: string,
    data: CreateProjectInvitationRequest,
    authHeader: string,
  ): Promise<ProjectInvitationCreateResponse> {
    const response =
      await this.backendClient.client.post<ProjectInvitationCreateResponse>(
        `/api/v1.0/projects/${projectId}/invitations`,
        data,
        this.backendClient.getAuthConfig(authHeader),
      );
    return response.data;
  }

  async getInvitations(
    projectId: string,
    page: number,
    size: number,
    authHeader: string,
  ): Promise<PageResponse<ProjectInvitationResponse>> {
    const response = await this.backendClient.client.get<
      PageResponse<ProjectInvitationResponse>
    >(`/api/v1.0/projects/${projectId}/invitations`, {
      ...this.backendClient.getAuthConfig(authHeader),
      params: {page, size},
    });
    return response.data;
  }

  async getMyInvitations(
    page: number,
    size: number,
    authHeader: string,
  ): Promise<PageResponse<ProjectInvitationResponse>> {
    const response = await this.backendClient.client.get<
      PageResponse<ProjectInvitationResponse>
    >('/api/v1.0/users/me/invitations/projects', {
      ...this.backendClient.getAuthConfig(authHeader),
      params: {page, size},
    });
    return response.data;
  }

  async acceptInvitation(
    invitationId: string,
    authHeader: string,
  ): Promise<ProjectMemberResponse> {
    const response =
      await this.backendClient.client.patch<ProjectMemberResponse>(
        `/api/v1.0/projects/invitations/${invitationId}/accept`,
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
      `/api/v1.0/projects/invitations/${invitationId}/reject`,
      {},
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }
}
