import type { ULID, DatabaseContext } from './common';

export interface ColumnResponse {
  id: ULID;
  tableId: ULID;
  name: string;
  dataType: string;
  lengthScale: string | null;
  isAutoIncrement: boolean;
  charset: string | null;
  collation: string | null;
  comment: string | null;
  ordinalPosition: number;
}

export interface CreateColumnRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  column: {
    id: ULID;
    tableId: ULID;
    name: string;
    ordinalPosition: number;
    dataType: string;
    lengthScale: string;
    charset: string;
    collation: string;
    comment: string;
  };
}

export interface UpdateColumnNameRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  columnId: ULID;
  newName: string;
}

export interface UpdateColumnTypeRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  columnId: ULID;
  dataType: string;
  lengthScale?: string;
}

export interface UpdateColumnPositionRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  columnId: ULID;
  newPosition: number;
}

export interface DeleteColumnRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  columnId: ULID;
}
