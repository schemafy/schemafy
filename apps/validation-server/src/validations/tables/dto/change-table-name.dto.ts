import type { Database, Schema, Table } from '@schemafy/validator';

export interface ChangeTableNameDto {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    newName: Table['name'];
}
