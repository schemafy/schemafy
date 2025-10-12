import type { Database, Index, Schema, Table } from '@schemafy/validator';

export interface ChangeIndexNameDto {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    indexId: Index['id'];
    newName: Index['name'];
}
