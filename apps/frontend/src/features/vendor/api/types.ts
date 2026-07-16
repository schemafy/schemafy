export type DbVendorSummary = {
  id: number;
  displayName: string;
  name: string;
  version: string;
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
};
