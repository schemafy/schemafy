import { Injectable } from '@nestjs/common';
import axios, { AxiosInstance } from 'axios';
import { ConfigService } from '@nestjs/config';

import { createHmacHeaders } from '../hmac/hmac.util';

@Injectable()
export class BackendClientService {
  readonly client: AxiosInstance;

  constructor(private readonly configService: ConfigService) {
    this.client = axios.create({
      baseURL: process.env.BACKEND_URL || 'http://localhost:8080',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
      withCredentials: true,
    });

    this.client.interceptors.request.use((config) => {
      const hmacSecret = this.configService.get<string>('HMAC_SECRET');

      if (!hmacSecret) {
        throw new Error('HMAC_SECRET is not defined');
      }

      const hmacHeaders = createHmacHeaders(hmacSecret, config);

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
