import type { ISODateString, ULID } from './common';
import type { TableResponse } from './table';

export interface SchemaResponse {
  id: ULID;
  projectId: ULID;
  dbVendorId: ULID;
  name: string;
  charset: string | null;
  collation: string | null;
  vendorOption: string | null;
  canvasViewport: string | null;
  createdAt: ISODateString;
  updatedAt: ISODateString;
}

export interface SchemaDetailResponse extends SchemaResponse {
  tables: TableResponse[];
}

export interface CreateSchemaRequest {
  projectId: ULID;
  name: string;
  dbVendor: string;
  charset?: string;
  collation?: string;
  vendorOption?: string;
}

export interface UpdateSchemaNameRequest {
  schemaId: ULID;
  name: string;
}

export interface DeleteSchemaRequest {
  schemaId: ULID;
}
