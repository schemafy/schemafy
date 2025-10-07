import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';
import type { ValidateResult } from '../common';
import { RelationshipsService } from './relationships.service';
import type {
  Database,
  Schema,
  Relationship,
  RelationshipColumn,
} from '@schemafy/validator';

@Controller()
export class RelationshipsController {
  constructor(private readonly service: RelationshipsService) {}

  @GrpcMethod('ValidationService', 'CreateRelationship')
  createRelationship(req: {
    database: Database;
    schemaId: Schema['id'];
    relationship: Relationship;
  }): ValidateResult {
    const { database, schemaId, relationship } = req;
    return this.service.createRelationship(database, schemaId, relationship);
  }

  @GrpcMethod('ValidationService', 'DeleteRelationship')
  deleteRelationship(req: {
    database: Database;
    schemaId: Schema['id'];
    relationshipId: Relationship['id'];
  }): ValidateResult {
    const { database, schemaId, relationshipId } = req;
    return this.service.deleteRelationship(database, schemaId, relationshipId);
  }

  @GrpcMethod('ValidationService', 'ChangeRelationshipName')
  changeRelationshipName(req: {
    database: Database;
    schemaId: Schema['id'];
    relationshipId: Relationship['id'];
    newName: Relationship['name'];
  }): ValidateResult {
    const { database, schemaId, relationshipId, newName } = req;
    return this.service.changeRelationshipName(
      database,
      schemaId,
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
  addColumnToRelationship(req: {
    database: Database;
    schemaId: Schema['id'];
    relationshipId: Relationship['id'];
    relationshipColumn: Omit<RelationshipColumn, 'relationshipId'>;
  }): ValidateResult {
    const { database, schemaId, relationshipId, relationshipColumn } = req;
    return this.service.addColumnToRelationship(
      database,
      schemaId,
      relationshipId,
      relationshipColumn,
    );
  }

  @GrpcMethod('ValidationService', 'RemoveColumnFromRelationship')
  removeColumnFromRelationship(req: {
    database: Database;
    schemaId: Schema['id'];
    relationshipId: Relationship['id'];
    relationshipColumnId: RelationshipColumn['id'];
  }): ValidateResult {
    const { database, schemaId, relationshipId, relationshipColumnId } = req;
    return this.service.removeColumnFromRelationship(
      database,
      schemaId,
      relationshipId,
      relationshipColumnId,
    );
  }
}
