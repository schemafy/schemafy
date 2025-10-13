import type { Database, Index, Schema, Table } from '@schemafy/validator';

export interface CreateIndexDto {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    index: Index;
}
