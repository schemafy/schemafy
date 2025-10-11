import type { Database, Column, Schema, Table } from '@schemafy/validator';

export interface ChangeColumnNullableDto {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  columnId: Column['id'];
  nullable: boolean;
}
