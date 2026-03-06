import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';
import type {
  ChangeTableExtraRequest,
  ChangeTableMetaRequest,
  ChangeTableNameRequest,
  CreateTableRequest,
  MutationResponse,
  TableResponse,
  TableSnapshotResponse,
} from './erd.types';

@Injectable()
export class TableService {
  constructor(private readonly backendClient: BackendClientService) {}

  async createTable(
    data: CreateTableRequest,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse<TableResponse>> {
    const response = await this.backendClient.client.post<
      MutationResponse<TableResponse>
    >('/api/v1.0/tables', data, this.backendClient.getAuthConfig(authHeader, sessionId));
    return response.data;
  }

  async getTable(tableId: string, authHeader: string): Promise<TableResponse> {
    const response = await this.backendClient.client.get<TableResponse>(
      `/api/v1.0/tables/${tableId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getTablesBySchemaId(
    schemaId: string,
    authHeader: string,
  ): Promise<TableResponse[]> {
    const response = await this.backendClient.client.get<TableResponse[]>(
      `/api/v1.0/schemas/${schemaId}/tables`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getTableSnapshot(
    tableId: string,
    authHeader: string,
  ): Promise<TableSnapshotResponse> {
    const response = await this.backendClient.client.get<TableSnapshotResponse>(
      `/api/v1.0/tables/${tableId}/snapshot`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getTableSnapshots(
    tableIds: string[],
    authHeader: string,
  ): Promise<Record<string, TableSnapshotResponse>> {
    const response = await this.backendClient.client.get<
      Record<string, TableSnapshotResponse>
    >('/api/v1.0/tables/snapshots', {
      ...this.backendClient.getAuthConfig(authHeader),
      params: { tableIds },
    });
    return response.data;
  }

  async changeTableName(
    tableId: string,
    data: ChangeTableNameRequest,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.patch<MutationResponse>(
      `/api/v1.0/tables/${tableId}/name`,
      data,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }

  async changeTableMeta(
    tableId: string,
    data: ChangeTableMetaRequest,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.patch<MutationResponse>(
      `/api/v1.0/tables/${tableId}/meta`,
      data,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }

  async changeTableExtra(
    tableId: string,
    data: ChangeTableExtraRequest,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.patch<MutationResponse>(
      `/api/v1.0/tables/${tableId}/extra`,
      data,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }

  async deleteTable(
    tableId: string,
    authHeader: string,
    sessionId?: string,
  ): Promise<MutationResponse> {
    const response = await this.backendClient.client.delete<MutationResponse>(
      `/api/v1.0/tables/${tableId}`,
      this.backendClient.getAuthConfig(authHeader, sessionId),
    );
    return response.data;
  }

  async getSchemaWithSnapshots(
    schemaId: string,
    authHeader: string,
  ): Promise<Record<string, TableSnapshotResponse>> {
    const tables = await this.getTablesBySchemaId(schemaId, authHeader);
    const tableIds = tables.map((t) => t.id);
    if (tableIds.length === 0) {
      return {};
    }
    return this.getTableSnapshots(tableIds, authHeader);
  }
}
