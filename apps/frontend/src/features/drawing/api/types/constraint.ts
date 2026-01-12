import type { ULID, DatabaseContext } from './common';

export type ConstraintColumnResponse = {
  id: ULID;
  constraintId: ULID;
  columnId: ULID;
  seqNo: number;
  isAffected: boolean;
};

export type ConstraintResponse = {
  id: ULID;
  tableId: ULID;
  name: string;
  kind: string;
  checkExpr?: string | null;
  defaultExpr?: string | null;
  columns: ConstraintColumnResponse[];
  isAffected: boolean;
};

export type CreateConstraintRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  constraint: {
    id: ULID;
    tableId: ULID;
    name: string;
    kind: string;
    columns: {
      id: ULID;
      constraintId: ULID;
      columnId: ULID;
      seqNo: number;
    }[];
  };
};

export type UpdateConstraintNameRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  constraintId: ULID;
  newName: string;
};

export type AddColumnToConstraintRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  constraintId: ULID;
  constraintColumn: {
    id: ULID;
    constraintId: ULID;
    columnId: ULID;
    seqNo: number;
  };
};

export type RemoveColumnFromConstraintRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  constraintId: ULID;
  constraintColumnId: ULID;
};

export type DeleteConstraintRequest = {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  constraintId: ULID;
};
