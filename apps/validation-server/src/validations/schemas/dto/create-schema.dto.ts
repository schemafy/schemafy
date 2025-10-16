import type { Database, Schema } from '@schemafy/validator';

export interface CreateSchemaDto {
  database: Database;
  schema: Schema;
}
