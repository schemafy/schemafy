import { Injectable } from '@nestjs/common';
import { ERD_VALIDATOR } from '@schemafy/validator';

import { toErrorDetails } from '../common/error-mapper';

import type { ValidateResult } from '../common';
import type {
  Database,
  Constraint,
  Schema,
  Table,
  ConstraintColumn,
} from '@schemafy/validator';

@Injectable()
export class ConstraintsService {
  createConstraint(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraint: Constraint,
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.createConstraint(
        database,
        schemaId,
        tableId,
        constraint,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  deleteConstraint(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.deleteConstraint(
        database,
        schemaId,
        tableId,
        constraintId,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  changeConstraintName(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    newName: Constraint['name'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeConstraintName(
        database,
        schemaId,
        tableId,
        constraintId,
        newName,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  addColumnToConstraint(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    constraintColumn: ConstraintColumn,
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.addColumnToConstraint(
        database,
        schemaId,
        tableId,
        constraintId,
        constraintColumn,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  removeColumnFromConstraint(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    constraintColumnId: ConstraintColumn['id'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.removeColumnFromConstraint(
        database,
        schemaId,
        tableId,
        constraintId,
        constraintColumnId,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }
}
