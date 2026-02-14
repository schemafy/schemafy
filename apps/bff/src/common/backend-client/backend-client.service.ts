import { Injectable } from '@nestjs/common';
import axios, { AxiosInstance } from 'axios';

import { createHmacHeaders } from '../hmac/hmac.util.js';

@Injectable()
export class BackendClientService {
  readonly client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: process.env.BACKEND_URL || 'http://localhost:8080',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
      withCredentials: true,
    });

    this.client.interceptors.request.use((config) => {
      const hmacSecret =
        process.env.HMAC_SECRET ||
        'default-hmac-secret-change-me-in-production';

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
