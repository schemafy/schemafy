import type { Database, Index, Schema, Table } from '@schemafy/validator';

export interface DeleteIndexDto {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    indexId: Index['id'];
}
