import { Injectable } from '@nestjs/common';
import axios, { AxiosInstance } from 'axios';

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
  }

  getAuthConfig(authHeader: string) {
    return {
      headers: { Authorization: authHeader },
    };
  }
}
