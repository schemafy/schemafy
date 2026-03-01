import { Injectable } from '@nestjs/common';
import axios, { AxiosInstance } from 'axios';
import { ConfigService } from '@nestjs/config';

import { createHmacHeaders } from '../hmac/hmac.util';

@Injectable()
export class BackendClientService {
  readonly client: AxiosInstance;
  private readonly hmacSecret: string;

  constructor(private readonly configService: ConfigService) {
    this.hmacSecret = this.configService.get<string>(
      'HMAC_SECRET',
      'default-hmac-secret-change-me-in-production',
    );

    this.client = axios.create({
      baseURL: this.configService.get<string>(
        'BACKEND_URL',
        'http://localhost:8080',
      ),
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
      withCredentials: true,
    });

    this.client.interceptors.request.use((config) => {
      const hmacHeaders = createHmacHeaders(this.hmacSecret, config);

      if (hmacHeaders) {
        Object.assign(config.headers, hmacHeaders);
      }

      return config;
    });
  }

  getAuthConfig(authHeader: string) {
    return {
      headers: { Authorization: authHeader },
    };
  }
}
