import type {
  Database,
  Relationship,
  RelationshipColumn,
  Schema,
} from '@schemafy/validator';

export interface RemoveColumnFromRelationshipDto {
  database: Database;
  schemaId: Schema['id'];
  relationshipId: Relationship['id'];
  relationshipColumnId: RelationshipColumn['id'];
}
