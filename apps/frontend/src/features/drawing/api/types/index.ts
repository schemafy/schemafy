import type { ULID, DatabaseContext } from './common';

export interface IndexColumnResponse {
  id: ULID;
  indexId: ULID;
  columnId: ULID;
  seqNo: number;
  sortDir: string;
  isAffected: boolean;
}

export interface IndexResponse {
  id: ULID;
  tableId: ULID;
  name: string;
  type: string;
  comment?: string | null;
  columns: IndexColumnResponse[];
  isAffected: boolean;
}

export interface CreateIndexRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  index: {
    id: ULID;
    tableId: ULID;
    name: string;
    type: string;
    comment: string;
    columns: {
      id: ULID;
      indexId: ULID;
      columnId: ULID;
      seqNo: number;
      sortDir: string;
    }[];
  };
}

export interface UpdateIndexNameRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  indexId: ULID;
  newName: string;
}

export interface AddColumnToIndexRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  indexId: ULID;
  indexColumn: {
    id: ULID;
    indexId: ULID;
    columnId: ULID;
    seqNo: number;
    sortDir?: string;
  };
}

export interface RemoveColumnFromIndexRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  indexId: ULID;
  indexColumnId: ULID;
}

export interface DeleteIndexRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  indexId: ULID;
}
