import type { Database, Column, Schema, Table } from '@schemafy/validator';

export interface DeleteColumnDto {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  columnId: Column['id'];
}
