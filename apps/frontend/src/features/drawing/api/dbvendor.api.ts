import { api } from '@/lib/api/helpers';
import type { DBVendorResponse, DbVendorListResponse } from './types/dbvendor';

export const getDbVendors = () => api.get<DbVendorListResponse>('/api/vendors');

export const getDbVendor = (displayName: string) =>
  api.get<DBVendorResponse>(`/api/vendors/${displayName}`);
