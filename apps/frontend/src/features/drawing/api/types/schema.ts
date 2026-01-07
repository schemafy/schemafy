import type { ISODateString, ULID, DatabaseContext } from './common';
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
  tables?: TableResponse[];
}

export interface CreateSchemaRequest {
  database: DatabaseContext;
  schema: {
    id: ULID;
    projectId: ULID;
    dbVendorId: string;
    name: string;
    charset: string;
    collation: string;
    vendorOption: string;
  };
}

export interface UpdateSchemaNameRequest {
  database: DatabaseContext;
  schemaId: ULID;
  newName: string;
}

export interface DeleteSchemaRequest {
  database: DatabaseContext;
  schemaId: ULID;
}
