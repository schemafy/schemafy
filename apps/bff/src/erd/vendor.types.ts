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

export type DatatypeParameterName = 'length' | 'precision' | 'scale' | 'values';

export type DatatypeParameterValueType = 'integer' | 'string_array';

export type DatatypeParameter = {
  name: DatatypeParameterName;
  label: string;
  valueType: DatatypeParameterValueType;
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

export type DbVendorDetailResponse = {
  id: number;
  displayName: string;
  name: string;
  version: string;
  datatypeMappings: DatatypePolicy;
  capabilities: VendorCapabilities;
};
