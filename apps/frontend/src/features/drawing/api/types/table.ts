import type { ISODateString, ULID, DatabaseContext } from './common';
import type { ColumnResponse } from './column';
import type { ConstraintResponse } from './constraint';
import type { IndexResponse } from './index';
import type { RelationshipResponse } from './relationship';

export type TableResponse = {
  id: ULID;
  schemaId: ULID;
  name: string;
  comment: string | null;
  tableOptions: string | null;
  extra: string | null;
  createdAt: ISODateString;
  updatedAt: ISODateString;
};

export type TableDetailResponse = TableResponse & {
  columns: ColumnResponse[];
  constraints: ConstraintResponse[];
  indexes: IndexResponse[];
  relationships: RelationshipResponse[];
};

export type CreateTableRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  table: {
    id: ULID;
    schemaId: ULID;
    name: string;
    comment: string;
    tableOptions: string;
  };
};

export type UpdateTableNameRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  newName: string;
};

export type UpdateTableExtraRequest = {
  extra: string;
};

export type DeleteTableRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
};
