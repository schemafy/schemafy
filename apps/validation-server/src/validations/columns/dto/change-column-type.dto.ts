import type { Database, Column, Schema, Table } from '@schemafy/validator';

export interface ChangeColumnTypeDto {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  columnId: Column['id'];
  dataType: Column['dataType'];
  lengthScale?: Column['lengthScale'];
}
