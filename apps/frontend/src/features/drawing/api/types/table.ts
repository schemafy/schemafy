import type { ISODateString, ULID, DatabaseContext } from './common';
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
  database: DatabaseContext;
  schemaId: ULID;
  table: {
    id: ULID;
    schemaId: ULID;
    name: string;
    comment: string;
    tableOptions: string;
  };
}

export interface UpdateTableNameRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  newName: string;
}

export interface UpdateTableExtraRequest {
  extra: string;
}

export interface DeleteTableRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
}
