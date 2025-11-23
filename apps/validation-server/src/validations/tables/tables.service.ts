import { Injectable, Logger } from '@nestjs/common';
import { ERD_VALIDATOR } from '@schemafy/validator';

import { toErrorDetails } from '../common/error-mapper';

import type { ValidateResult } from '../common';
import type { Database, Table, Schema } from '@schemafy/validator';

@Injectable()
export class TablesService {
  private readonly logger = new Logger(TablesService.name);
  createTable(
    database: Database,
    schemaId: Schema['id'],
    table: Omit<Table, 'schemaId' | 'createdAt' | 'updatedAt'>,
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.createTable(database, schemaId, table);
      this.logger.log(
        `CreateTable request successfully validated, schemaId: ${schemaId}, table: ${JSON.stringify(table)}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `CreateTable request failed, schemaId: ${schemaId}, table: ${JSON.stringify(table)}`,
        err,
      );
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  deleteTable(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.deleteTable(database, schemaId, tableId);
      this.logger.log(
        `DeleteTable request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `DeleteTable request failed, schemaId: ${schemaId}, tableId: ${tableId}`,
        err,
      );
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  changeTableName(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    newName: Table['name'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeTableName(
        database,
        schemaId,
        tableId,
        newName,
      );
      this.logger.log(
        `ChangeTableName request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, newName: ${newName}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `ChangeTableName request failed, schemaId: ${schemaId}, tableId: ${tableId}, newName: ${newName}`,
        err,
      );
      return { failure: { errors: toErrorDetails(err) } };
    }
  }
}
