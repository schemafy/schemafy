import { Injectable } from '@nestjs/common';
import { BackendClientService } from 'src/common/backend-client/backend-client.service';
import { AuthResponse } from './auth.types';

@Injectable()
export class AuthService {
  constructor(private readonly backendClient: BackendClientService) {}

  async getMyInfo(authHeader: string): Promise<AuthResponse> {
    const response = await this.backendClient.client.get<AuthResponse>(
      '/api/v1.0/users',
      this.backendClient.getAuthConfig(authHeader),
    );

    return response.data;
  }

  async logout(
    authHeader: string,
  ): Promise<{ data: null; setCookies: string[] }> {
    const response = await this.backendClient.client.post(
      '/api/v1.0/users/logout',
      {},
      this.backendClient.getAuthConfig(authHeader),
    );

    const setCookies = response.headers['set-cookie'] ?? [];

    return { data: response.data, setCookies };
  }
}
