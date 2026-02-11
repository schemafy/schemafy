import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';
import { ApiResponse } from '../common/types/api-response.types';
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
  ): Promise<ApiResponse<MutationResponse<ConstraintResponse>>> {
    const response = await this.backendClient.client.post<
      ApiResponse<MutationResponse<ConstraintResponse>>
    >('/api/v1.0/constraints', data, this.backendClient.getAuthConfig(authHeader));
    return response.data;
  }

  async getConstraint(
    constraintId: string,
    authHeader: string,
  ): Promise<ApiResponse<ConstraintResponse>> {
    const response = await this.backendClient.client.get<
      ApiResponse<ConstraintResponse>
    >(
      `/api/v1.0/constraints/${constraintId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getConstraintsByTableId(
    tableId: string,
    authHeader: string,
  ): Promise<ApiResponse<ConstraintResponse[]>> {
    const response = await this.backendClient.client.get<
      ApiResponse<ConstraintResponse[]>
    >(
      `/api/v1.0/tables/${tableId}/constraints`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeConstraintName(
    constraintId: string,
    data: ChangeConstraintNameRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/constraints/${constraintId}/name`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeConstraintCheckExpr(
    constraintId: string,
    data: ChangeConstraintCheckExprRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/constraints/${constraintId}/check-expr`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeConstraintDefaultExpr(
    constraintId: string,
    data: ChangeConstraintDefaultExprRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/constraints/${constraintId}/default-expr`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async deleteConstraint(
    constraintId: string,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.delete<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/constraints/${constraintId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getConstraintColumns(
    constraintId: string,
    authHeader: string,
  ): Promise<ApiResponse<ConstraintColumnResponse[]>> {
    const response = await this.backendClient.client.get<
      ApiResponse<ConstraintColumnResponse[]>
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
  ): Promise<ApiResponse<MutationResponse<AddConstraintColumnResponse>>> {
    const response = await this.backendClient.client.post<
      ApiResponse<MutationResponse<AddConstraintColumnResponse>>
    >(
      `/api/v1.0/constraints/${constraintId}/columns`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async removeConstraintColumn(
    constraintColumnId: string,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.delete<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/constraint-columns/${constraintColumnId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getConstraintColumn(
    constraintColumnId: string,
    authHeader: string,
  ): Promise<ApiResponse<ConstraintColumnResponse>> {
    const response = await this.backendClient.client.get<
      ApiResponse<ConstraintColumnResponse>
    >(
      `/api/v1.0/constraint-columns/${constraintColumnId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeConstraintColumnPosition(
    constraintColumnId: string,
    data: ChangeConstraintColumnPositionRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/constraint-columns/${constraintColumnId}/position`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }
}
