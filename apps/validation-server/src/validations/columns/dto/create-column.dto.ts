import type { Database, Column, Schema, Table } from '@schemafy/validator';

export interface CreateColumnDto {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  column: Column;
}
