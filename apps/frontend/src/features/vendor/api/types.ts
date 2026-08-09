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
  name: 'length' | 'precision' | 'scale' | 'values';
  label: string;
  valueType: 'integer' | 'string_array';
  required: boolean;
  order: number;
  minValue: number | null;
  maxValue: number | null;
  minItems: number | null;
  maxItems: number | null;
  minItemLength: number | null;
  maxItemLength: number | null;
};

export type DatatypeProperties = {
  autoIncrementAllowed: boolean;
  charsetCollationAllowed: boolean;
  indexTypes: IndexType[];
  foreignKeyGroup: string | null;
};

export type VendorDatatype = {
  sqlType: string;
  aliases: string[];
  displayName: string;
  category: string;
  parameters: DatatypeParameter[];
  sqlDeclarationTemplate: string;
  properties: DatatypeProperties;
};

export type DatatypePolicy = {
  schemaVersion: 2;
  vendor: string;
  version: string | null;
  versionRange: string | null;
  types: VendorDatatype[];
};

export type DbVendorDetail = {
  id: number;
  displayName: string;
  name: string;
  version: string;
  datatypeMappings: DatatypePolicy;
  capabilities: VendorCapabilities;
};
