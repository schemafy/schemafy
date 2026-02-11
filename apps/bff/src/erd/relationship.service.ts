import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';
import { ApiResponse } from '../common/types/api-response.types';
import type {
  AddRelationshipColumnRequest,
  AddRelationshipColumnResponse,
  ChangeRelationshipCardinalityRequest,
  ChangeRelationshipColumnPositionRequest,
  ChangeRelationshipExtraRequest,
  ChangeRelationshipKindRequest,
  ChangeRelationshipNameRequest,
  CreateRelationshipRequest,
  MutationResponse,
  RelationshipColumnResponse,
  RelationshipResponse,
} from './erd.types';

@Injectable()
export class RelationshipService {
  constructor(private readonly backendClient: BackendClientService) {}

  async createRelationship(
    data: CreateRelationshipRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse<RelationshipResponse>>> {
    const response = await this.backendClient.client.post<
      ApiResponse<MutationResponse<RelationshipResponse>>
    >('/api/v1.0/relationships', data, this.backendClient.getAuthConfig(authHeader));
    return response.data;
  }

  async getRelationship(
    relationshipId: string,
    authHeader: string,
  ): Promise<ApiResponse<RelationshipResponse>> {
    const response = await this.backendClient.client.get<
      ApiResponse<RelationshipResponse>
    >(
      `/api/v1.0/relationships/${relationshipId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getRelationshipsByTableId(
    tableId: string,
    authHeader: string,
  ): Promise<ApiResponse<RelationshipResponse[]>> {
    const response = await this.backendClient.client.get<
      ApiResponse<RelationshipResponse[]>
    >(
      `/api/v1.0/tables/${tableId}/relationships`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeRelationshipName(
    relationshipId: string,
    data: ChangeRelationshipNameRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/relationships/${relationshipId}/name`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeRelationshipKind(
    relationshipId: string,
    data: ChangeRelationshipKindRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/relationships/${relationshipId}/kind`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeRelationshipCardinality(
    relationshipId: string,
    data: ChangeRelationshipCardinalityRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/relationships/${relationshipId}/cardinality`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeRelationshipExtra(
    relationshipId: string,
    data: ChangeRelationshipExtraRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/relationships/${relationshipId}/extra`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async deleteRelationship(
    relationshipId: string,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.delete<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/relationships/${relationshipId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getRelationshipColumns(
    relationshipId: string,
    authHeader: string,
  ): Promise<ApiResponse<RelationshipColumnResponse[]>> {
    const response = await this.backendClient.client.get<
      ApiResponse<RelationshipColumnResponse[]>
    >(
      `/api/v1.0/relationships/${relationshipId}/columns`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async addRelationshipColumn(
    relationshipId: string,
    data: AddRelationshipColumnRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse<AddRelationshipColumnResponse>>> {
    const response = await this.backendClient.client.post<
      ApiResponse<MutationResponse<AddRelationshipColumnResponse>>
    >(
      `/api/v1.0/relationships/${relationshipId}/columns`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async removeRelationshipColumn(
    relationshipColumnId: string,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.delete<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/relationship-columns/${relationshipColumnId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getRelationshipColumn(
    relationshipColumnId: string,
    authHeader: string,
  ): Promise<ApiResponse<RelationshipColumnResponse>> {
    const response = await this.backendClient.client.get<
      ApiResponse<RelationshipColumnResponse>
    >(
      `/api/v1.0/relationship-columns/${relationshipColumnId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeRelationshipColumnPosition(
    relationshipColumnId: string,
    data: ChangeRelationshipColumnPositionRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/relationship-columns/${relationshipColumnId}/position`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }
}
