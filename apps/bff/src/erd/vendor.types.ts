import type { IndexType } from './erd.types';

export type DbVendorSummaryResponse = {
  id: number;
  displayName: string;
  name: string;
  version: string;
};

export type VendorCapabilities = {
  schemaVersion: number;
  indexes: {
    supportedTypes: IndexType[];
    sortDirectionTypes: IndexType[];
  };
};

export type DbVendorDetailResponse = {
  id: number;
  displayName: string;
  name: string;
  version: string;
  datatypeMappings: unknown;
  capabilities: VendorCapabilities;
};
