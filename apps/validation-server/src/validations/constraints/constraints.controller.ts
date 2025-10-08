import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { ConstraintsService } from './constraints.service';

import type { ValidateResult } from '../common';
import type {
  Database,
  Schema,
  Table,
  Constraint,
  ConstraintColumn,
} from '@schemafy/validator';

@Controller()
export class ConstraintsController {
  constructor(private readonly service: ConstraintsService) {}

  @GrpcMethod('ValidationService', 'CreateConstraint')
  createConstraint(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    constraint: Constraint;
  }): ValidateResult {
    const { database, schemaId, tableId, constraint } = req;
    return this.service.createConstraint(
      database,
      schemaId,
      tableId,
      constraint,
    );
  }

  @GrpcMethod('ValidationService', 'DeleteConstraint')
  deleteConstraint(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    constraintId: Constraint['id'];
  }): ValidateResult {
    const { database, schemaId, tableId, constraintId } = req;
    return this.service.deleteConstraint(
      database,
      schemaId,
      tableId,
      constraintId,
    );
  }

  @GrpcMethod('ValidationService', 'ChangeConstraintName')
  changeConstraintName(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    constraintId: Constraint['id'];
    newName: Constraint['name'];
  }): ValidateResult {
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
  addColumnToConstraint(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    constraintId: Constraint['id'];
    constraintColumn: ConstraintColumn;
  }): ValidateResult {
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
  removeColumnFromConstraint(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    constraintId: Constraint['id'];
    constraintColumnId: ConstraintColumn['id'];
  }): ValidateResult {
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
