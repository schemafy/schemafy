import type { Database, Constraint, ConstraintColumn, Schema, Table } from '@schemafy/validator';

export interface RemoveColumnFromConstraintDto {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  constraintId: Constraint['id'];
  constraintColumnId: ConstraintColumn['id'];
}
