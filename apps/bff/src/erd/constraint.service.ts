import { Injectable } from '@nestjs/common';
import {
  BackendClientService,
  type CollaborationRequestHeaders,
} from '../common/backend-client/backend-client.service';
import type {
  AddConstraintColumnRequest,
  AddConstraintColumnResponse,
  ChangeConstraintCheckExprRequest,
  ChangeConstraintColumnPositionRequest,
  ChangeConstraintDefaultExprRequest,
  ChangeConstraintNameRequest,
  ConstraintColumnResponse,
  ConstraintResponse,
  CreateConstraintRequest,
  MutationResponse,
} from './erd.types';

@Injectable()
export class ConstraintService {
  constructor(private readonly backendClient: BackendClientService) {}

  async createConstraint(
    data: CreateConstraintRequest,
    authHeader: string,
    collaborationHeaders?: CollaborationRequestHeaders,
  ): Promise<MutationResponse<ConstraintResponse>> {
    const response = await this.backendClient.client.post<
      MutationResponse<ConstraintResponse>
    >(
      '/api/v1.0/constraints',
      data,
      this.backendClient.getAuthConfig(authHeader, collaborationHeaders),
    );
    return response.data;
  }

  async getConstraint(
    constraintId: string,
    authHeader: string,
  ): Promise<ConstraintResponse> {
    const response = await this.backendClient.client.get<ConstraintResponse>(
      `/api/v1.0/constraints/${constraintId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getConstraintsByTableId(
    tableId: string,
    authHeader: string,
  ): Promise<ConstraintResponse[]> {
    const response = await this.backendClient.client.get<ConstraintResponse[]>(
      `/api/v1.0/tables/${tableId}/constraints`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeConstraintName(
    constraintId: string,
    data: ChangeConstraintNameRequest,
    authHeader: string,
    collaborationHeaders?: CollaborationRequestHeaders,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.patch<MutationResponse>(
      `/api/v1.0/constraints/${constraintId}/name`,
      data,
      this.backendClient.getAuthConfig(authHeader, collaborationHeaders),
    );
    return response.data;
  }

  async changeConstraintCheckExpr(
    constraintId: string,
    data: ChangeConstraintCheckExprRequest,
    authHeader: string,
    collaborationHeaders?: CollaborationRequestHeaders,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.patch<MutationResponse>(
      `/api/v1.0/constraints/${constraintId}/check-expr`,
      data,
      this.backendClient.getAuthConfig(authHeader, collaborationHeaders),
    );
    return response.data;
  }

  async changeConstraintDefaultExpr(
    constraintId: string,
    data: ChangeConstraintDefaultExprRequest,
    authHeader: string,
    collaborationHeaders?: CollaborationRequestHeaders,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.patch<MutationResponse>(
      `/api/v1.0/constraints/${constraintId}/default-expr`,
      data,
      this.backendClient.getAuthConfig(authHeader, collaborationHeaders),
    );
    return response.data;
  }

  async deleteConstraint(
    constraintId: string,
    authHeader: string,
    collaborationHeaders?: CollaborationRequestHeaders,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.delete<MutationResponse>(
      `/api/v1.0/constraints/${constraintId}`,
      this.backendClient.getAuthConfig(authHeader, collaborationHeaders),
    );
    return response.data;
  }

  async getConstraintColumns(
    constraintId: string,
    authHeader: string,
  ): Promise<ConstraintColumnResponse[]> {
    const response = await this.backendClient.client.get<
      ConstraintColumnResponse[]
    >(
      `/api/v1.0/constraints/${constraintId}/columns`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async addConstraintColumn(
    constraintId: string,
    data: AddConstraintColumnRequest,
    authHeader: string,
    collaborationHeaders?: CollaborationRequestHeaders,
  ): Promise<MutationResponse<AddConstraintColumnResponse>> {
    const response = await this.backendClient.client.post<
      MutationResponse<AddConstraintColumnResponse>
    >(
      `/api/v1.0/constraints/${constraintId}/columns`,
      data,
      this.backendClient.getAuthConfig(authHeader, collaborationHeaders),
    );
    return response.data;
  }

  async removeConstraintColumn(
    constraintColumnId: string,
    authHeader: string,
    collaborationHeaders?: CollaborationRequestHeaders,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.delete<MutationResponse>(
      `/api/v1.0/constraint-columns/${constraintColumnId}`,
      this.backendClient.getAuthConfig(authHeader, collaborationHeaders),
    );
    return response.data;
  }

  async getConstraintColumn(
    constraintColumnId: string,
    authHeader: string,
  ): Promise<ConstraintColumnResponse> {
    const response =
      await this.backendClient.client.get<ConstraintColumnResponse>(
        `/api/v1.0/constraint-columns/${constraintColumnId}`,
        this.backendClient.getAuthConfig(authHeader),
      );
    return response.data;
  }

  async changeConstraintColumnPosition(
    constraintColumnId: string,
    data: ChangeConstraintColumnPositionRequest,
    authHeader: string,
    collaborationHeaders?: CollaborationRequestHeaders,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.patch<MutationResponse>(
      `/api/v1.0/constraint-columns/${constraintColumnId}/position`,
      data,
      this.backendClient.getAuthConfig(authHeader, collaborationHeaders),
    );
    return response.data;
  }
}
