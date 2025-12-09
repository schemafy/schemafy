import type { ULID } from './common';

export interface ConstraintColumnResponse {
  id: ULID;
  constraintId: ULID;
  columnId: ULID;
  seqNo: number;
}

export interface ConstraintResponse {
  id: ULID;
  tableId: ULID;
  name: string;
  kind: string;
  columns: ConstraintColumnResponse[];
}

export interface CreateConstraintRequest {
  tableId: ULID;
  name: string;
  kind: string;
  columns?: CreateConstraintColumnRequest[];
}

export interface CreateConstraintColumnRequest {
  columnId: ULID;
  seqNo: number;
}

export interface UpdateConstraintNameRequest {
  constraintId: ULID;
  name: string;
}

export interface AddColumnToConstraintRequest {
  constraintId: ULID;
  columnId: ULID;
  seqNo: number;
}

export interface RemoveColumnFromConstraintRequest {
  constraintId: ULID;
  constraintColumnId: ULID;
}

export interface DeleteConstraintRequest {
  constraintId: ULID;
}
