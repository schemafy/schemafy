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
  identifiers: {
    maxLength: number;
    lengthUnit: 'CODE_POINTS' | 'UTF8_BYTES';
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
