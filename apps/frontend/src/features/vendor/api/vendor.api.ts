import { apiClient } from '@/lib/api';
import type { DbVendorDetail, DbVendorSummary } from './types';

export const listVendors = async (): Promise<DbVendorSummary[]> => {
  const response = await apiClient.get<DbVendorSummary[]>('/vendors');
  return response.data;
};

export const getVendor = async (id: number): Promise<DbVendorDetail> => {
  const response = await apiClient.get<DbVendorDetail>(`/vendors/${id}`);
  return response.data;
};
