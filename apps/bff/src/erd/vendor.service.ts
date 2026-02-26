import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';
import { ApiResponse } from '../common/types/api-response.types';
import type {
  DbVendorDetailResponse,
  DbVendorSummaryResponse,
} from './vendor.types';

@Injectable()
export class VendorService {
  constructor(private readonly backendClient: BackendClientService) {}

  async listVendors(
    authHeader: string,
  ): Promise<ApiResponse<DbVendorSummaryResponse[]>> {
    const response = await this.backendClient.client.get<
      ApiResponse<DbVendorSummaryResponse[]>
    >('/api/v1.0/vendors', this.backendClient.getAuthConfig(authHeader));
    return response.data;
  }

  async getVendor(
    displayName: string,
    authHeader: string,
  ): Promise<ApiResponse<DbVendorDetailResponse>> {
    const response = await this.backendClient.client.get<
      ApiResponse<DbVendorDetailResponse>
    >(
      `/api/v1.0/vendors/${displayName}`,
      this.backendClient.getAuthConfig(authHeader),
    );
    return response.data;
  }
}
