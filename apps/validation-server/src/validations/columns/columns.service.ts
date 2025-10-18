import { Injectable, Logger } from '@nestjs/common';
import { ERD_VALIDATOR } from '@schemafy/validator';

import { toErrorDetails } from '../common/error-mapper';

import type { ValidateResult } from '../common';
import type { Database, Column, Schema, Table } from '@schemafy/validator';

@Injectable()
export class ColumnsService {
  private readonly logger = new Logger(ColumnsService.name);

  createColumn(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    column: Omit<Column, 'tableId' | 'createdAt' | 'updatedAt'>,
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.createColumn(database, schemaId, tableId, column);
      this.logger.log(
        `CreateColumn request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, column: ${JSON.stringify(column)}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `CreateColumn request failed, schemaId: ${schemaId}, tableId: ${tableId}, column: ${JSON.stringify(column)}`,
        err,
      );
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  deleteColumn(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.deleteColumn(database, schemaId, tableId, columnId);
      this.logger.log(
        `DeleteColumn request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, columnId: ${columnId}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `DeleteColumn request failed, schemaId: ${schemaId}, tableId: ${tableId}, columnId: ${columnId}`,
        err,
      );
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  changeColumnName(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    newName: Column['name'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeColumnName(database, schemaId, tableId, columnId, newName);
      this.logger.log(
        `ChangeColumnName request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, columnId: ${columnId}, newName: ${newName}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `ChangeColumnName request failed, schemaId: ${schemaId}, tableId: ${tableId}, columnId: ${columnId}, newName: ${newName}`,
        err,
      );
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  changeColumnType(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    dataType: Column['dataType'],
    lengthScale?: Column['lengthScale'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeColumnType(database, schemaId, tableId, columnId, dataType, lengthScale);
      this.logger.log(
        `ChangeColumnType request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, columnId: ${columnId}, dataType: ${dataType}, lengthScale: ${JSON.stringify(
          lengthScale,
        )}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `ChangeColumnType request failed, schemaId: ${schemaId}, tableId: ${tableId}, columnId: ${columnId}, dataType: ${dataType}, lengthScale: ${JSON.stringify(
          lengthScale,
        )}`,
        err,
      );
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  changeColumnPosition(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    newPosition: Column['ordinalPosition'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeColumnPosition(database, schemaId, tableId, columnId, newPosition);
      this.logger.log(
        `ChangeColumnPosition request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, columnId: ${columnId}, newPosition: ${newPosition}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `ChangeColumnPosition request failed, schemaId: ${schemaId}, tableId: ${tableId}, columnId: ${columnId}, newPosition: ${newPosition}`,
        err,
      );
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  changeColumnNullable(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    nullable: boolean,
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeColumnNullable(database, schemaId, tableId, columnId, nullable);
      this.logger.log(
        `ChangeColumnNullable request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, columnId: ${columnId}, nullable: ${nullable}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `ChangeColumnNullable request failed, schemaId: ${schemaId}, tableId: ${tableId}, columnId: ${columnId}, nullable: ${nullable}`,
        err,
      );
      return { failure: { errors: toErrorDetails(err) } };
    }
  }
}
