import type {
  Database,
  Constraint,
  ConstraintColumn,
  Schema,
  Table,
} from '@schemafy/validator';

export interface AddColumnToConstraintDto {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  constraintId: Constraint['id'];
  constraintColumn: ConstraintColumn;
}
