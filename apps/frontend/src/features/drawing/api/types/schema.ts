import type { ISODateString, ULID, DatabaseContext } from './common';
import type { TableResponse } from './table';

export type SchemaResponse = {
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
};

export type SchemaDetailResponse = SchemaResponse & {
  tables?: TableResponse[];
};

export type CreateSchemaRequest = {
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
};

export type UpdateSchemaNameRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  newName: string;
};

export type DeleteSchemaRequest = {
  database: DatabaseContext;
  schemaId: ULID;
};
