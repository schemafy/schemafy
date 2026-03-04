import { apiClient } from '@/lib/api';
import type { DbVendorSummary, DbVendorDetail } from './types';

export const listVendors = async (): Promise<DbVendorSummary[]> => {
  const response = await apiClient.get<DbVendorSummary[]>('/vendors');
  return response.data;
};

export const getVendor = async (
  displayName: string,
): Promise<DbVendorDetail> => {
  const response = await apiClient.get<DbVendorDetail>(
    `/vendors/${encodeURIComponent(displayName)}`,
  );
  return response.data;
};
