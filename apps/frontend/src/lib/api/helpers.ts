import type { AxiosRequestConfig } from 'axios';
import { apiClient } from './client';
import type { ApiResponse } from './types';

async function request<T>(
  method: 'get' | 'post' | 'put' | 'delete',
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig,
): Promise<ApiResponse<T>> {
  const response = await apiClient.request<ApiResponse<T>>({
    method,
    url,
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
