import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';
import { ApiResponse } from '../common/types/api-response.types';
import type {
  ChangeSchemaNameRequest,
  CreateSchemaRequest,
  MutationResponse,
  SchemaResponse,
} from './erd.types';

@Injectable()
export class SchemaService {
  constructor(private readonly backendClient: BackendClientService) {}

  async createSchema(
    data: CreateSchemaRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse<SchemaResponse>>> {
    const response = await this.backendClient.client.post<
      ApiResponse<MutationResponse<SchemaResponse>>
    >('/api/v1.0/schemas', data, this.backendClient.getAuthConfig(authHeader));
    return response.data;
  }

  async getSchema(
    schemaId: string,
    authHeader: string,
  ): Promise<ApiResponse<SchemaResponse>> {
    const response = await this.backendClient.client.get<
      ApiResponse<SchemaResponse>
    >(
      `/api/v1.0/schemas/${schemaId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeSchemaName(
    schemaId: string,
    data: ChangeSchemaNameRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/schemas/${schemaId}/name`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async deleteSchema(
    schemaId: string,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.delete<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/schemas/${schemaId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }
}
