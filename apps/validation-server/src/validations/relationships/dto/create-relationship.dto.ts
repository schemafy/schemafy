import type { Database, Relationship, Schema } from '@schemafy/validator';

export interface CreateRelationshipDto {
    database: Database;
    schemaId: Schema['id'];
    relationship: Relationship;
}
