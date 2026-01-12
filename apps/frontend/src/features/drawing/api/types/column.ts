import type { ULID, DatabaseContext } from './common';

export type ColumnResponse = {
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
};

export type CreateColumnRequest = {
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
};

export type UpdateColumnNameRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  columnId: ULID;
  newName: string;
};

export type UpdateColumnTypeRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  columnId: ULID;
  dataType: string;
  lengthScale?: string;
};

export type UpdateColumnPositionRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  columnId: ULID;
  newPosition: number;
};

export type DeleteColumnRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  columnId: ULID;
};
