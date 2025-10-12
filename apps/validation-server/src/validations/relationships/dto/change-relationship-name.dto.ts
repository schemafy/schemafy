import type { Database, Relationship, Schema } from '@schemafy/validator';

export interface ChangeRelationshipNameDto {
    database: Database;
    schemaId: Schema['id'];
    relationshipId: Relationship['id'];
    newName: Relationship['name'];
}
