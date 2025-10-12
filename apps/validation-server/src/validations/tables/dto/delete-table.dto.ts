import type { Database, Schema, Table } from '@schemafy/validator';

export interface DeleteTableDto {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
}
