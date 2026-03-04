import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';
import type {
  DbVendorDetailResponse,
  DbVendorSummaryResponse,
} from './vendor.types';

@Injectable()
export class VendorService {
  constructor(private readonly backendClient: BackendClientService) {}

  async listVendors(authHeader: string): Promise<DbVendorSummaryResponse[]> {
    const response = await this.backendClient.client.get<
      DbVendorSummaryResponse[]
    >('/api/v1.0/vendors', this.backendClient.getAuthConfig(authHeader));
    return response.data;
  }

  async getVendor(
    displayName: string,
    authHeader: string,
  ): Promise<DbVendorDetailResponse> {
    const response =
      await this.backendClient.client.get<DbVendorDetailResponse>(
        `/api/v1.0/vendors/${displayName}`,
        this.backendClient.getAuthConfig(authHeader),
      );
    return response.data;
  }
}
