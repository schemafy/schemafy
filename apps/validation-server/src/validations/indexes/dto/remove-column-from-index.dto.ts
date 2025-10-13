import type { Database, Index, IndexColumn, Schema, Table } from '@schemafy/validator';

export interface RemoveColumnFromIndexDto {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    indexId: Index['id'];
    indexColumnId: IndexColumn['id'];
}
