import type { ISODateString, ULID } from './common';
import type { ColumnResponse } from './column';
import type { ConstraintResponse } from './constraint';
import type { IndexResponse } from './index';
import type { RelationshipResponse } from './relationship';

export interface TableResponse {
  id: ULID;
  schemaId: ULID;
  name: string;
  comment: string | null;
  tableOptions: string | null;
  extra: string | null;
  createdAt: ISODateString;
  updatedAt: ISODateString;
}

export interface TableDetailResponse extends TableResponse {
  columns: ColumnResponse[];
  constraints: ConstraintResponse[];
  indexes: IndexResponse[];
  relationships: RelationshipResponse[];
}

export interface CreateTableRequest {
  schemaId: ULID;
  name: string;
  comment?: string;
  tableOptions?: string;
}

export interface UpdateTableNameRequest {
  tableId: ULID;
  name: string;
}

export interface DeleteTableRequest {
  tableId: ULID;
}
