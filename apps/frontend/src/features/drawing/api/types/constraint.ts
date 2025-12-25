import type { ULID, DatabaseContext } from './common';

export interface ConstraintColumnResponse {
  id: ULID;
  constraintId: ULID;
  columnId: ULID;
  seqNo: number;
  isAffected: boolean;
}

export interface ConstraintResponse {
  id: ULID;
  tableId: ULID;
  name: string;
  kind: string;
  checkExpr?: string | null;
  defaultExpr?: string | null;
  columns: ConstraintColumnResponse[];
  isAffected: boolean;
}

export interface CreateConstraintRequest {
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
}

export interface UpdateConstraintNameRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  constraintId: ULID;
  newName: string;
}

export interface AddColumnToConstraintRequest {
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
}

export interface RemoveColumnFromConstraintRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  constraintId: ULID;
  constraintColumnId: ULID;
}

export interface DeleteConstraintRequest {
  database: DatabaseContext;
  schemaId: ULID;
  tableId: ULID;
  constraintId: ULID;
}
