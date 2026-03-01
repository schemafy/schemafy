import { apiClient } from '@/lib/api';
import type { ApiResponse } from '@/lib/api';
import type { DbVendorSummary, DbVendorDetail } from './types';

export const listVendors = async (): Promise<
  ApiResponse<DbVendorSummary[]>
> => {
  const response =
    await apiClient.get<ApiResponse<DbVendorSummary[]>>('/vendors');
  return response.data;
};

export const getVendor = async (
  displayName: string,
): Promise<ApiResponse<DbVendorDetail>> => {
  const response = await apiClient.get<ApiResponse<DbVendorDetail>>(
    `/vendors/${displayName}`,
  );
  return response.data;
};
