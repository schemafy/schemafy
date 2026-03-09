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
      paramsSerializer: (params) => {
        const searchParams = new URLSearchParams();
        Object.entries(params as Record<string, unknown>).forEach(
          ([key, value]) => {
            if (Array.isArray(value)) {
              value.forEach((item) => searchParams.append(key, String(item)));
            } else if (value !== undefined && value !== null) {
              searchParams.append(key, String(value));
            }
          },
        );
        return searchParams.toString();
      },
    });

    this.client.interceptors.request.use((config) => {
      const hmacHeaders = createHmacHeaders(this.hmacSecret, config);

      if (hmacHeaders) {
        Object.assign(config.headers, hmacHeaders);
      }

      return config;
    });
  }

  getAuthConfig(authHeader: string, sessionId?: string) {
    const headers: Record<string, string> = { Authorization: authHeader };
    if (sessionId) {
      headers['X-Session-Id'] = sessionId;
    }
    return { headers };
  }
}
