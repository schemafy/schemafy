import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { RelationshipsService } from './relationships.service';

import type { ValidateResult } from '../common';
import type {
  AddColumnPairToRelationshipRequest,
  ChangeRelationshipNameRequest,
  CreateRelationshipRequest,
  DeleteRelationshipRequest,
  RemoveColumnPairFromRelationshipRequest,
} from '../../types/validation.types';
import type { Database, Relationship, Schema } from '@schemafy/validator';

@Controller()
export class RelationshipsController {
  constructor(private readonly service: RelationshipsService) {}

  @GrpcMethod('ValidationService', 'CreateRelationship')
  createRelationship(req: CreateRelationshipRequest): ValidateResult {
    const { database, relationship } = req;
    return this.service.createRelationship(
      database,
      relationship.sourceSchemaId,
      relationship,
    );
  }

  @GrpcMethod('ValidationService', 'DeleteRelationship')
  deleteRelationship(req: DeleteRelationshipRequest): ValidateResult {
    const { database, relationshipId } = req;
    // Note: schemaId is needed by service but not in proto - extracting from relationship
    const relationship = database.schemas
      .flatMap((s) => s.relationships || [])
      .find((r) => r.id === relationshipId);
    if (!relationship) {
      return {
        failure: {
          errors: [
            {
              code: 'NOT_FOUND',
              message: `Relationship ${relationshipId} not found`,
            },
          ],
        },
      };
    }
    return this.service.deleteRelationship(
      database,
      relationship.sourceSchemaId,
      relationshipId,
    );
  }

  @GrpcMethod('ValidationService', 'ChangeRelationshipName')
  changeRelationshipName(req: ChangeRelationshipNameRequest): ValidateResult {
    const { database, relationshipId, newName } = req;
    // Note: schemaId is needed by service but not in proto - extracting from relationship
    const relationship = database.schemas
      .flatMap((s) => s.relationships || [])
      .find((r) => r.id === relationshipId);
    if (!relationship) {
      return {
        failure: {
          errors: [
            {
              code: 'NOT_FOUND',
              message: `Relationship ${relationshipId} not found`,
            },
          ],
        },
      };
    }
    return this.service.changeRelationshipName(
      database,
      relationship.sourceSchemaId,
      relationshipId,
      newName,
    );
  }

  @GrpcMethod('ValidationService', 'ChangeRelationshipCardinality')
  changeRelationshipCardinality(req: {
    database: Database;
    schemaId: Schema['id'];
    relationshipId: Relationship['id'];
    cardinality: Relationship['cardinality'];
  }): ValidateResult {
    const { database, schemaId, relationshipId, cardinality } = req;
    return this.service.changeRelationshipCardinality(
      database,
      schemaId,
      relationshipId,
      cardinality,
    );
  }

  @GrpcMethod('ValidationService', 'AddColumnToRelationship')
  addColumnToRelationship(
    req: AddColumnPairToRelationshipRequest,
  ): ValidateResult {
    const { database, relationshipId, columnPair } = req;
    // Note: schemaId is needed by service but not in proto - extracting from relationship
    const relationship = database.schemas
      .flatMap((s) => s.relationships || [])
      .find((r) => r.id === relationshipId);
    if (!relationship) {
      return {
        failure: {
          errors: [
            {
              code: 'NOT_FOUND',
              message: `Relationship ${relationshipId} not found`,
            },
          ],
        },
      };
    }
    return this.service.addColumnToRelationship(
      database,
      relationship.sourceSchemaId,
      relationshipId,
      columnPair,
    );
  }

  @GrpcMethod('ValidationService', 'RemoveColumnFromRelationship')
  removeColumnFromRelationship(
    req: RemoveColumnPairFromRelationshipRequest,
  ): ValidateResult {
    const { database, relationshipId, relationshipColumnId } = req;
    // Note: schemaId is needed by service but not in proto - extracting from relationship
    const relationship = database.schemas
      .flatMap((s) => s.relationships || [])
      .find((r) => r.id === relationshipId);
    if (!relationship) {
      return {
        failure: {
          errors: [
            {
              code: 'NOT_FOUND',
              message: `Relationship ${relationshipId} not found`,
            },
          ],
        },
      };
    }
    return this.service.removeColumnFromRelationship(
      database,
      relationship.sourceSchemaId,
      relationshipId,
      relationshipColumnId,
    );
  }
}
