import type { Database, Schema, Table } from '@schemafy/validator';

export interface CreateTableDto {
    database: Database;
    schemaId: Schema['id'];
    table: Table;
}
