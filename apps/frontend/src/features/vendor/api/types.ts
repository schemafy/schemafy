import type { IndexType } from '@/types/erd.types';

export type DbVendorSummary = {
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

export type DatatypeParameter = {
  name: string;
  label: string;
  valueType: 'integer' | 'string_array';
  required: boolean;
  order: number;
};

export type VendorDatatype = {
  sqlType: string;
  displayName: string;
  category: string;
  sqlDeclarationTemplate?: string;
  parameters: DatatypeParameter[];
};

export type DatatypeMappings = {
  schemaVersion: number;
  vendor: string;
  versionRange: string;
  types: VendorDatatype[];
};

export type DbVendorDetail = {
  id: number;
  displayName: string;
  name: string;
  version: string;
  datatypeMappings: DatatypeMappings;
  capabilities: VendorCapabilities;
};
