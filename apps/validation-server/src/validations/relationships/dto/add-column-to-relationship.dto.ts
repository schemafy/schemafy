import type {
  Database,
  Relationship,
  RelationshipColumn,
  Schema,
} from '@schemafy/validator';

export interface AddColumnToRelationshipDto {
  database: Database;
  schemaId: Schema['id'];
  relationshipId: Relationship['id'];
  relationshipColumn: RelationshipColumn;
}
