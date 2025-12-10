import type { ISODateString } from './common';

export interface DatatypeParameter {
  name: string;
  label: string;
  valueType: string;
  required: boolean;
  order: number;
}

export interface DatatypeDefinition {
  sqlType: string;
  displayName: string;
  category: string;
  sqlDeclarationTemplate?: string;
  parameters: DatatypeParameter[];
}

export interface DatatypeMappings {
  schemaVersion: number;
  vendor: string;
  versionRange: string;
  types: DatatypeDefinition[];
}

export interface DBVendorResponse {
  id: string;
  name: string;
  version: string;
  datatypeMappings: string;
  createdAt: ISODateString;
  updatedAt: ISODateString;
}

export interface DBVendorDetailResponse
  extends Omit<DBVendorResponse, 'datatypeMappings'> {
  datatypeMappings: DatatypeMappings;
}

export type DbVendorListResponse = DBVendorResponse[];
