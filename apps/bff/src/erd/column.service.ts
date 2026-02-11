import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';
import { ApiResponse } from '../common/types/api-response.types';
import type {
  ChangeColumnMetaRequest,
  ChangeColumnNameRequest,
  ChangeColumnPositionRequest,
  ChangeColumnTypeRequest,
  ColumnResponse,
  CreateColumnRequest,
  MutationResponse,
} from './erd.types';

@Injectable()
export class ColumnService {
  constructor(private readonly backendClient: BackendClientService) {}

  async createColumn(
    data: CreateColumnRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse<ColumnResponse>>> {
    const response = await this.backendClient.client.post<
      ApiResponse<MutationResponse<ColumnResponse>>
    >('/api/v1.0/columns', data, this.backendClient.getAuthConfig(authHeader));
    return response.data;
  }

  async getColumn(
    columnId: string,
    authHeader: string,
  ): Promise<ApiResponse<ColumnResponse>> {
    const response = await this.backendClient.client.get<
      ApiResponse<ColumnResponse>
    >(
      `/api/v1.0/columns/${columnId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async getColumnsByTableId(
    tableId: string,
    authHeader: string,
  ): Promise<ApiResponse<ColumnResponse[]>> {
    const response = await this.backendClient.client.get<
      ApiResponse<ColumnResponse[]>
    >(
      `/api/v1.0/tables/${tableId}/columns`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeColumnName(
    columnId: string,
    data: ChangeColumnNameRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/columns/${columnId}/name`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeColumnType(
    columnId: string,
    data: ChangeColumnTypeRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/columns/${columnId}/type`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeColumnMeta(
    columnId: string,
    data: ChangeColumnMetaRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/columns/${columnId}/meta`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async changeColumnPosition(
    columnId: string,
    data: ChangeColumnPositionRequest,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.patch<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/columns/${columnId}/position`,
      data,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }

  async deleteColumn(
    columnId: string,
    authHeader: string,
  ): Promise<ApiResponse<MutationResponse>> {
    const response = await this.backendClient.client.delete<
      ApiResponse<MutationResponse>
    >(
      `/api/v1.0/columns/${columnId}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }
}
