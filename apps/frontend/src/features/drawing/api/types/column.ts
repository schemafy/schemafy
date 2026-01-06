import type { ULID, DatabaseContext } from './common';

export interface ColumnResponse {
  id: ULID;
  tableId: ULID;
  name: string;
  seqNo: number;
  dataType?: string | null;
  lengthScale: string;
  isAutoIncrement: boolean;
  charset: string;
  collation: string;
  comment?: string | null;
  isAffected: boolean;
}

export interface CreateColumnRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  column: {
    id: ULID;
    tableId: ULID;
    name: string;
    seqNo: number;
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
