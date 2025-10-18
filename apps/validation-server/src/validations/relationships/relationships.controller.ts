import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { RelationshipsService } from './relationships.service';

import type {
  AddColumnToRelationshipDto,
  ChangeRelationshipCardinalityDto,
  ChangeRelationshipNameDto,
  CreateRelationshipDto,
  DeleteRelationshipDto,
  RemoveColumnFromRelationshipDto,
} from './dto';
import type { ValidateResult } from '../common';

@Controller()
export class RelationshipsController {
  constructor(private readonly service: RelationshipsService) {}

  @GrpcMethod('ValidationService', 'CreateRelationship')
  createRelationship(req: CreateRelationshipDto): ValidateResult {
    const { database, schemaId, relationship } = req;
    return this.service.createRelationship(database, schemaId, relationship);
  }

  @GrpcMethod('ValidationService', 'DeleteRelationship')
  deleteRelationship(req: DeleteRelationshipDto): ValidateResult {
    const { database, schemaId, relationshipId } = req;
    return this.service.deleteRelationship(database, schemaId, relationshipId);
  }

  @GrpcMethod('ValidationService', 'ChangeRelationshipName')
  changeRelationshipName(req: ChangeRelationshipNameDto): ValidateResult {
    const { database, schemaId, relationshipId, newName } = req;
    return this.service.changeRelationshipName(database, schemaId, relationshipId, newName);
  }

  @GrpcMethod('ValidationService', 'ChangeRelationshipCardinality')
  changeRelationshipCardinality(req: ChangeRelationshipCardinalityDto): ValidateResult {
    const { database, schemaId, relationshipId, cardinality } = req;
    return this.service.changeRelationshipCardinality(database, schemaId, relationshipId, cardinality);
  }

  @GrpcMethod('ValidationService', 'AddColumnToRelationship')
  addColumnToRelationship(req: AddColumnToRelationshipDto): ValidateResult {
    const { database, schemaId, relationshipId, relationshipColumn } = req;
    return this.service.addColumnToRelationship(database, schemaId, relationshipId, relationshipColumn);
  }

  @GrpcMethod('ValidationService', 'RemoveColumnFromRelationship')
  removeColumnFromRelationship(req: RemoveColumnFromRelationshipDto): ValidateResult {
    const { database, schemaId, relationshipId, relationshipColumnId } = req;
    return this.service.removeColumnFromRelationship(database, schemaId, relationshipId, relationshipColumnId);
  }
}
