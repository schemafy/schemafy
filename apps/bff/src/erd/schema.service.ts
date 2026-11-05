import { Injectable } from '@nestjs/common';
import {
  BackendClientService,
  type CollaborationRequestHeaders,
} from '../common/backend-client/backend-client.service';
import type {
  ChangeSchemaNameRequest,
  CreateSchemaRequest,
  MutationResponse,
  SchemaDdlExportResponse,
  SchemaMermaidExportResponse,
  SchemaResponse,
  SchemaSnapshotsResponse,
} from './erd.types';

const SCHEMA_EXPORT_TIMEOUT_MS = 30_000;

@Injectable()
export class SchemaService {
  constructor(private readonly backendClient: BackendClientService) {}

  async createSchema(
    data: CreateSchemaRequest,
    authHeader: string,
    collaborationHeaders?: CollaborationRequestHeaders,
  ): Promise<MutationResponse<SchemaResponse>> {
    const response = await this.backendClient.client.post<
      MutationResponse<SchemaResponse>
    >(
      '/api/v1.0/schemas',
      data,
      this.backendClient.getAuthConfig(authHeader, collaborationHeaders),
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

  async getSchemaWithSnapshots(
    schemaId: string,
    authHeader: string,
  ): Promise<SchemaSnapshotsResponse> {
    const response =
      await this.backendClient.client.get<SchemaSnapshotsResponse>(
        `/api/v1.0/schemas/${schemaId}/snapshots`,
        this.backendClient.getAuthConfig(authHeader),
      );
    return response.data;
  }

  async exportSchemaDdl(
    schemaId: string,
    targetDbVendor: string,
    authHeader: string,
    signal: AbortSignal,
  ): Promise<SchemaDdlExportResponse> {
    const response =
      await this.backendClient.client.get<SchemaDdlExportResponse>(
        `/api/v1.0/schemas/${schemaId}/exports/ddl`,
        {
          ...this.backendClient.getAuthConfig(authHeader),
          params: { targetDbVendor },
          timeout: SCHEMA_EXPORT_TIMEOUT_MS,
          signal,
        },
      );
    return response.data;
  }

  async exportSchemaMermaid(
    schemaId: string,
    authHeader: string,
    signal: AbortSignal,
  ): Promise<SchemaMermaidExportResponse> {
    const response =
      await this.backendClient.client.get<SchemaMermaidExportResponse>(
        `/api/v1.0/schemas/${schemaId}/exports/mermaid`,
        {
          ...this.backendClient.getAuthConfig(authHeader),
          timeout: SCHEMA_EXPORT_TIMEOUT_MS,
          signal,
        },
      );
    return response.data;
  }

  async changeSchemaName(
    schemaId: string,
    data: ChangeSchemaNameRequest,
    authHeader: string,
    collaborationHeaders?: CollaborationRequestHeaders,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.patch<MutationResponse>(
      `/api/v1.0/schemas/${schemaId}/name`,
      data,
      this.backendClient.getAuthConfig(authHeader, collaborationHeaders),
    );
    return response.data;
  }

  async deleteSchema(
    schemaId: string,
    authHeader: string,
    collaborationHeaders?: CollaborationRequestHeaders,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.delete<MutationResponse>(
      `/api/v1.0/schemas/${schemaId}`,
      this.backendClient.getAuthConfig(authHeader, collaborationHeaders),
    );
    return response.data;
  }
}
