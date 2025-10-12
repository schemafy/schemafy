import type { Database, Relationship, Schema } from '@schemafy/validator';

export interface ChangeRelationshipCardinalityDto {
    database: Database;
    schemaId: Schema['id'];
    relationshipId: Relationship['id'];
    cardinality: Relationship['cardinality'];
}
