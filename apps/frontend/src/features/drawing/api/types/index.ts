import type { ULID, DatabaseContext } from './common';

export type IndexColumnResponse = {
  id: ULID;
  indexId: ULID;
  columnId: ULID;
  seqNo: number;
  sortDir: string;
  isAffected: boolean;
};

export type IndexResponse = {
  id: ULID;
  tableId: ULID;
  name: string;
  type: string;
  comment?: string | null;
  columns: IndexColumnResponse[];
  isAffected: boolean;
};

export type CreateIndexRequest = {
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
};

export type UpdateIndexNameRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  indexId: ULID;
  newName: string;
};

export type AddColumnToIndexRequest = {
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
};

export type RemoveColumnFromIndexRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  indexId: ULID;
  indexColumnId: ULID;
};

export type UpdateIndexTypeRequest = {
  type: string;
};

export type UpdateIndexColumnSortDirRequest = {
  sortDir: string;
};

export type DeleteIndexRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  indexId: ULID;
};
