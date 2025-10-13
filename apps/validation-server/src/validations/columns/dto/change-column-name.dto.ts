import type { Database, Column, Schema, Table } from '@schemafy/validator';

export interface ChangeColumnNameDto {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    columnId: Column['id'];
    newName: Column['name'];
}
