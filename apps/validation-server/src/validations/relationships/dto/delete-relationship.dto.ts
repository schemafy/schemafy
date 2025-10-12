import type { Database, Relationship, Schema } from '@schemafy/validator';

export interface DeleteRelationshipDto {
    database: Database;
    schemaId: Schema['id'];
    relationshipId: Relationship['id'];
}
