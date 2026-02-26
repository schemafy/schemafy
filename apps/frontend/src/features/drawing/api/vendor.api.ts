import { bffClient } from '@/lib/api/bff-client';
import type { ApiResponse } from '@/lib/api';
import type { DbVendorSummary, DbVendorDetail } from './vendor.types';

export const listVendors = async (): Promise<
  ApiResponse<DbVendorSummary[]>
> => {
  const response =
    await bffClient.get<ApiResponse<DbVendorSummary[]>>('/vendors');
  return response.data;
};

export const getVendor = async (
  displayName: string,
): Promise<ApiResponse<DbVendorDetail>> => {
  const response = await bffClient.get<ApiResponse<DbVendorDetail>>(
    `/vendors/${displayName}`,
  );
  return response.data;
};
