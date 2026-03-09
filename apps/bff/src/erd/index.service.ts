import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';
import type {
  AddIndexColumnRequest,
  AddIndexColumnResponse,
  ChangeIndexColumnPositionRequest,
  ChangeIndexColumnSortDirectionRequest,
  ChangeIndexNameRequest,
  ChangeIndexTypeRequest,
  CreateIndexRequest,
  IndexColumnResponse,
  IndexResponse,
  MutationResponse,
} from './erd.types';

@Injectable()
export class IndexService {
  constructor(private readonly backendClient: BackendClientService) {}

  async createIndex(
    data: CreateIndexRequest,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse<IndexResponse>> {
    const response = await this.backendClient.client.post<
      MutationResponse<IndexResponse>
    >(
      '/api/v1.0/indexes',
      data,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }

  async getIndex(indexId: string, authHeader: string): Promise<IndexResponse> {
    const response = await this.backendClient.client.get<IndexResponse>(
      `/api/v1.0/indexes/${indexId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getIndexesByTableId(
    tableId: string,
    authHeader: string,
  ): Promise<IndexResponse[]> {
    const response = await this.backendClient.client.get<IndexResponse[]>(
      `/api/v1.0/tables/${tableId}/indexes`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeIndexName(
    indexId: string,
    data: ChangeIndexNameRequest,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.patch<MutationResponse>(
      `/api/v1.0/indexes/${indexId}/name`,
      data,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }

  async changeIndexType(
    indexId: string,
    data: ChangeIndexTypeRequest,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.patch<MutationResponse>(
      `/api/v1.0/indexes/${indexId}/type`,
      data,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }

  async deleteIndex(
    indexId: string,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.delete<MutationResponse>(
      `/api/v1.0/indexes/${indexId}`,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }

  async getIndexColumns(
    indexId: string,
    authHeader: string,
  ): Promise<IndexColumnResponse[]> {
    const response = await this.backendClient.client.get<IndexColumnResponse[]>(
      `/api/v1.0/indexes/${indexId}/columns`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async addIndexColumn(
    indexId: string,
    data: AddIndexColumnRequest,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse<AddIndexColumnResponse>> {
    const response = await this.backendClient.client.post<
      MutationResponse<AddIndexColumnResponse>
    >(
      `/api/v1.0/indexes/${indexId}/columns`,
      data,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }

  async removeIndexColumn(
    indexColumnId: string,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.delete<MutationResponse>(
      `/api/v1.0/index-columns/${indexColumnId}`,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }

  async getIndexColumn(
    indexColumnId: string,
    authHeader: string,
  ): Promise<IndexColumnResponse> {
    const response = await this.backendClient.client.get<IndexColumnResponse>(
      `/api/v1.0/index-columns/${indexColumnId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeIndexColumnPosition(
    indexColumnId: string,
    data: ChangeIndexColumnPositionRequest,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.patch<MutationResponse>(
      `/api/v1.0/index-columns/${indexColumnId}/position`,
      data,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }

  async changeIndexColumnSortDirection(
    indexColumnId: string,
    data: ChangeIndexColumnSortDirectionRequest,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.patch<MutationResponse>(
      `/api/v1.0/index-columns/${indexColumnId}/sort-direction`,
      data,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }
}
