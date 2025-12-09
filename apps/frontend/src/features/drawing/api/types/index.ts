import type { ULID } from './common';

export interface IndexColumnResponse {
  id: ULID;
  indexId: ULID;
  columnId: ULID;
  seqNo: number;
  sortDir: string | null;
}

export interface IndexResponse {
  id: ULID;
  tableId: ULID;
  name: string;
  type: string;
  comment: string | null;
  columns: IndexColumnResponse[];
}

export interface CreateIndexRequest {
  tableId: ULID;
  name: string;
  type: string;
  comment?: string;
  columns?: CreateIndexColumnRequest[];
}

export interface CreateIndexColumnRequest {
  columnId: ULID;
  seqNo: number;
  sortDir?: string;
}

export interface UpdateIndexNameRequest {
  indexId: ULID;
  name: string;
}

export interface AddColumnToIndexRequest {
  indexId: ULID;
  columnId: ULID;
  seqNo: number;
  sortDir?: string;
}

export interface RemoveColumnFromIndexRequest {
  indexId: ULID;
  indexColumnId: ULID;
}

export interface DeleteIndexRequest {
  indexId: ULID;
}
