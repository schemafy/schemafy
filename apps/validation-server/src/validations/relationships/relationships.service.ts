import { Injectable, Logger } from '@nestjs/common';
import { ERD_VALIDATOR } from '@schemafy/validator';

import { toErrorDetails } from '../common/error-mapper';

import type { ValidateResult } from '../common';
import type {
  Database,
  Schema,
  Relationship,
  RelationshipColumn,
} from '@schemafy/validator';

@Injectable()
export class RelationshipsService {
  private readonly logger = new Logger(RelationshipsService.name);
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
      this.logger.log(
        `CreateRelationship request successfully validated, schemaId: ${schemaId}, relationship: ${JSON.stringify(
          relationship,
        )}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `CreateRelationship request failed, schemaId: ${schemaId}, relationship: ${JSON.stringify(relationship)}`,
        err,
      );
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
      this.logger.log(
        `DeleteRelationship request successfully validated, schemaId: ${schemaId}, relationshipId: ${relationshipId}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `DeleteRelationship request failed, schemaId: ${schemaId}, relationshipId: ${relationshipId}`,
        err,
      );
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
      this.logger.log(
        `ChangeRelationshipName request successfully validated, schemaId: ${schemaId}, relationshipId: ${relationshipId}, newName: ${newName}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `ChangeRelationshipName request failed, schemaId: ${schemaId}, relationshipId: ${relationshipId}, newName: ${newName}`,
        err,
      );
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
      this.logger.log(
        `ChangeRelationshipCardinality request successfully validated, schemaId: ${schemaId}, relationshipId: ${relationshipId}, cardinality: ${cardinality}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `ChangeRelationshipCardinality request failed, schemaId: ${schemaId}, relationshipId: ${relationshipId}, cardinality: ${cardinality}`,
        err,
      );
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
      this.logger.log(
        `AddColumnToRelationship request successfully validated, schemaId: ${schemaId}, relationshipId: ${relationshipId}, relationshipColumn: ${JSON.stringify(
          relationshipColumn,
        )}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `AddColumnToRelationship request failed, schemaId: ${schemaId}, relationshipId: ${relationshipId}, relationshipColumn: ${JSON.stringify(
          relationshipColumn,
        )}`,
        err,
      );
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
      this.logger.log(
        `RemoveColumnFromRelationship request successfully validated, schemaId: ${schemaId}, relationshipId: ${relationshipId}, relationshipColumnId: ${relationshipColumnId}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `RemoveColumnFromRelationship request failed, schemaId: ${schemaId}, relationshipId: ${relationshipId}, relationshipColumnId: ${relationshipColumnId}`,
        err,
      );
      return { failure: { errors: toErrorDetails(err) } };
    }
  }
}
