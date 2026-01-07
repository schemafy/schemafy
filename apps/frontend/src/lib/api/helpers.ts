import type { AxiosRequestConfig } from 'axios';
import { apiClient } from './client';
import type { ApiResponse } from './types';

const API_VERSION = 'v1.0';
const API_BASE_PATH = `/api/${API_VERSION}`;

async function request<T>(
  method: 'get' | 'post' | 'put' | 'delete',
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig,
): Promise<ApiResponse<T>> {
  const response = await apiClient.request<ApiResponse<T>>({
    method,
    url: `${API_BASE_PATH}${url}`,
    data,
    ...config,
  });
  return response.data;
}

export const api = {
  get: <T>(url: string, config?: AxiosRequestConfig) =>
    request<T>('get', url, undefined, config),

  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request<T>('post', url, data, config),

  put: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request<T>('put', url, data, config),

  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    request<T>('delete', url, undefined, config),
};
