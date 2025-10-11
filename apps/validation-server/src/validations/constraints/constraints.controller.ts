import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { ConstraintsService } from './constraints.service';

import type { ValidateResult } from '../common';
import type {
  AddColumnToConstraintRequest,
  ChangeConstraintNameRequest,
  CreateConstraintRequest,
  DeleteConstraintRequest,
  RemoveColumnFromConstraintRequest,
} from '../../types/validation.types';

@Controller()
export class ConstraintsController {
  constructor(private readonly service: ConstraintsService) {}

  @GrpcMethod('ValidationService', 'CreateConstraint')
  createConstraint(req: CreateConstraintRequest): ValidateResult {
    const { database, schemaId, tableId, constraint } = req;
    return this.service.createConstraint(
      database,
      schemaId,
      tableId,
      constraint,
    );
  }

  @GrpcMethod('ValidationService', 'DeleteConstraint')
  deleteConstraint(req: DeleteConstraintRequest): ValidateResult {
    const { database, schemaId, tableId, constraintId } = req;
    return this.service.deleteConstraint(
      database,
      schemaId,
      tableId,
      constraintId,
    );
  }

  @GrpcMethod('ValidationService', 'ChangeConstraintName')
  changeConstraintName(req: ChangeConstraintNameRequest): ValidateResult {
    const { database, schemaId, tableId, constraintId, newName } = req;
    return this.service.changeConstraintName(
      database,
      schemaId,
      tableId,
      constraintId,
      newName,
    );
  }

  @GrpcMethod('ValidationService', 'AddColumnToConstraint')
  addColumnToConstraint(req: AddColumnToConstraintRequest): ValidateResult {
    const { database, schemaId, tableId, constraintId, constraintColumn } = req;
    return this.service.addColumnToConstraint(
      database,
      schemaId,
      tableId,
      constraintId,
      constraintColumn,
    );
  }

  @GrpcMethod('ValidationService', 'RemoveColumnFromConstraint')
  removeColumnFromConstraint(
    req: RemoveColumnFromConstraintRequest,
  ): ValidateResult {
    const { database, schemaId, tableId, constraintId, constraintColumnId } =
      req;
    return this.service.removeColumnFromConstraint(
      database,
      schemaId,
      tableId,
      constraintId,
      constraintColumnId,
    );
  }
}
