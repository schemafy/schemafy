import type { Database, Relationship, Schema } from '@schemafy/validator';

export interface ChangeRelationshipKindDto {
  database: Database;
  schemaId: Schema['id'];
  relationshipId: Relationship['id'];
  kind: Relationship['kind'];
}
