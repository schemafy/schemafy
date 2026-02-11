import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';
import { ApiResponse } from '../common/types/api-response.types';
import type {
  ChangeSchemaNameRequest,
  ChangeTableExtraRequest,
  ChangeTableMetaRequest,
  ChangeTableNameRequest,
  CreateSchemaRequest,
  CreateTableRequest,
  MutationResponse,
  SchemaResponse,
  TableResponse,
  TableSnapshotResponse,
} from './erd.types';

@Injectable()
export class ErdService {
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

  async createTable(
    data: CreateTableRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse<TableResponse>>> {
    const response = await this.backendClient.client.post<
      ApiResponse<MutationResponse<TableResponse>>
    >('/api/v1.0/tables', data, this.backendClient.getAuthConfig(authHeader));
    return response.data;
  }

  async getTable(
    tableId: string,
    authHeader: string,
  ): Promise<ApiResponse<TableResponse>> {
    const response = await this.backendClient.client.get<
      ApiResponse<TableResponse>
    >(
      `/api/v1.0/tables/${tableId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getTablesBySchemaId(
    schemaId: string,
    authHeader: string,
  ): Promise<ApiResponse<TableResponse[]>> {
    const response = await this.backendClient.client.get<
      ApiResponse<TableResponse[]>
    >(
      `/api/v1.0/schemas/${schemaId}/tables`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getTableSnapshot(
    tableId: string,
    authHeader: string,
  ): Promise<ApiResponse<TableSnapshotResponse>> {
    const response = await this.backendClient.client.get<
      ApiResponse<TableSnapshotResponse>
    >(
      `/api/v1.0/tables/${tableId}/snapshot`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getTableSnapshots(
    tableIds: string[],
    authHeader: string,
  ): Promise<ApiResponse<Record<string, TableSnapshotResponse>>> {
    const response = await this.backendClient.client.get<
      ApiResponse<Record<string, TableSnapshotResponse>>
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
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/tables/${tableId}/name`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeTableMeta(
    tableId: string,
    data: ChangeTableMetaRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/tables/${tableId}/meta`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeTableExtra(
    tableId: string,
    data: ChangeTableExtraRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/tables/${tableId}/extra`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async deleteTable(
    tableId: string,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.delete<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/tables/${tableId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getSchemaWithSnapshots(
    schemaId: string,
    authHeader: string,
  ): Promise<ApiResponse<Record<string, TableSnapshotResponse>>> {
    const tablesRes = await this.getTablesBySchemaId(schemaId, authHeader);

    if (!tablesRes.success || !tablesRes.result) {
      return { success: tablesRes.success, result: null, error: tablesRes.error };
    }

    const tableIds = tablesRes.result.map((t) => t.id);
    if (tableIds.length === 0) {
      return { success: true, result: {}, error: null };
    }

    return this.getTableSnapshots(tableIds, authHeader);
  }
}
