import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';
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
    sessionId?: string,
  ): Promise<MutationResponse<SchemaResponse>> {
    const response = await this.backendClient.client.post<
      MutationResponse<SchemaResponse>
    >(
      '/api/v1.0/schemas',
      data,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }

  async getSchemasByProjectId(
    projectId: string,
    authHeader: string,
  ): Promise<SchemaResponse[]> {
    const response = await this.backendClient.client.get<SchemaResponse[]>(
      `/api/v1.0/projects/${projectId}/schemas`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getSchema(
    schemaId: string,
    authHeader: string,
  ): Promise<SchemaResponse> {
    const response = await this.backendClient.client.get<SchemaResponse>(
      `/api/v1.0/schemas/${schemaId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeSchemaName(
    schemaId: string,
    data: ChangeSchemaNameRequest,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.patch<MutationResponse>(
      `/api/v1.0/schemas/${schemaId}/name`,
      data,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }

  async deleteSchema(
    schemaId: string,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.delete<MutationResponse>(
      `/api/v1.0/schemas/${schemaId}`,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }
}
