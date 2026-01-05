import { Injectable, Logger } from '@nestjs/common';
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
  private readonly logger = new Logger(ConstraintsService.name);
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
      this.logger.log(
        `CreateConstraint request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, constraint: ${JSON.stringify(
          constraint,
        )}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `CreateConstraint request failed, schemaId: ${schemaId}, tableId: ${tableId}, constraint: ${JSON.stringify(
          constraint,
        )}`,
        err,
      );
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
      this.logger.log(
        `DeleteConstraint request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, constraintId: ${constraintId}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `DeleteConstraint request failed, schemaId: ${schemaId}, tableId: ${tableId}, constraintId: ${constraintId}`,
        err,
      );
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
      this.logger.log(
        `ChangeConstraintName request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, constraintId: ${constraintId}, newName: ${newName}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `ChangeConstraintName request failed, schemaId: ${schemaId}, tableId: ${tableId}, constraintId: ${constraintId}, newName: ${newName}`,
        err,
      );
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
      this.logger.log(
        `AddColumnToConstraint request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, constraintId: ${constraintId}, constraintColumn: ${JSON.stringify(
          constraintColumn,
        )}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `AddColumnToConstraint request failed, schemaId: ${schemaId}, tableId: ${tableId}, constraintId: ${constraintId}, constraintColumn: ${JSON.stringify(
          constraintColumn,
        )}`,
        err,
      );
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
      this.logger.log(
        `RemoveColumnFromConstraint request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, constraintId: ${constraintId}, constraintColumnId: ${constraintColumnId}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `RemoveColumnFromConstraint request failed, schemaId: ${schemaId}, tableId: ${tableId}, constraintId: ${constraintId}, constraintColumnId: ${constraintColumnId}`,
        err,
      );
      return { failure: { errors: toErrorDetails(err) } };
    }
  }
}
