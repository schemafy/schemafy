import type { Database, Schema } from '@schemafy/validator';

export interface DeleteSchemaDto {
  database: Database;
  schemaId: Schema['id'];
}
