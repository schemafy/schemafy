import { Injectable } from '@nestjs/common';
import { BackendClientService } from 'src/common/backend-client/backend-client.service';
import { AuthResponse } from './auth.types';
import { ApiResponse } from 'src/common/types/api-response.types';

@Injectable()
export class AuthService {
  constructor(private readonly backendClient: BackendClientService) {}

  async getMyInfo(authHeader: string): Promise<ApiResponse<AuthResponse>> {
    const response = await this.backendClient.client.get<
      ApiResponse<AuthResponse>
    >('/users', this.backendClient.getAuthConfig(authHeader));

    return response.data;
  }

  async logout(authHeader: string): Promise<ApiResponse<null>> {
    const response = await this.backendClient.client.post<ApiResponse<null>>(
      '/users/logout',
      this.backendClient.getAuthConfig(authHeader),
    );

    return response.data;
  }
}
