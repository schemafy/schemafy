import type { Database, Column, Schema, Table } from '@schemafy/validator';

export interface ChangeColumnPositionDto {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  columnId: Column['id'];
  newPosition: Column['ordinalPosition'];
}
