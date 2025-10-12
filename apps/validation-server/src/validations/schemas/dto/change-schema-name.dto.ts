import type { Database, Schema } from '@schemafy/validator';

export interface ChangeSchemaNameDto {
    database: Database;
    schemaId: Schema['id'];
    newName: Schema['name'];
}
