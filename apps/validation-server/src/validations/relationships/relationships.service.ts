import { Injectable } from '@nestjs/common';
import type { ValidateResult } from '../common';
import { toErrorDetails } from '../common/error-mapper';
import type {
  Database,
  Schema,
  Relationship,
  RelationshipColumn,
} from '@schemafy/validator';
import { ERD_VALIDATOR } from '@schemafy/validator';

@Injectable()
export class RelationshipsService {
  createRelationship(
    database: Database,
    schemaId: Schema['id'],
    relationship: Relationship,
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.createRelationship(
        database,
        schemaId,
        relationship,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  deleteRelationship(
    database: Database,
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.deleteRelationship(
        database,
        schemaId,
        relationshipId,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  changeRelationshipName(
    database: Database,
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    newName: Relationship['name'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeRelationshipName(
        database,
        schemaId,
        relationshipId,
        newName,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  changeRelationshipCardinality(
    database: Database,
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    cardinality: Relationship['cardinality'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeRelationshipCardinality(
        database,
        schemaId,
        relationshipId,
        cardinality,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  addColumnToRelationship(
    database: Database,
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    relationshipColumn: Omit<RelationshipColumn, 'relationshipId'>,
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.addColumnToRelationship(
        database,
        schemaId,
        relationshipId,
        relationshipColumn,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  removeColumnFromRelationship(
    database: Database,
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    relationshipColumnId: RelationshipColumn['id'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.removeColumnFromRelationship(
        database,
        schemaId,
        relationshipId,
        relationshipColumnId,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }
}
