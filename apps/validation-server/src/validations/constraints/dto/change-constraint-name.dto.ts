import type { Database, Constraint, Schema, Table } from '@schemafy/validator';

export interface ChangeConstraintNameDto {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  constraintId: Constraint['id'];
  newName: Constraint['name'];
}
